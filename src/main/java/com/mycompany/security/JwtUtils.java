package com.mycompany.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import com.mycompany.config.JwtProperties;
import com.mycompany.config.TokenProperties;
import com.mycompany.entity.UserEntity;
import com.mycompany.enums.EnumRole;
import com.mycompany.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class JwtUtils {

    SecretKey secretKey;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    TokenProperties tokenProperties;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CustomUserDetailsService customUserDetailsService;

    @PostConstruct
    public void initializeSecretKey() {
        readJwtSecret();
    }

    public SecretKey readJwtSecret() {
        if (secretKey != null) {
            return secretKey;
        }
        String secretToUse = null;

        String jwtSecretFile = jwtProperties.getSecretFile();
        if (jwtSecretFile != null && !jwtSecretFile.isEmpty()) {
            try {
                String fileSecret = Files.readString(Paths.get(jwtSecretFile)).trim();
                if (!fileSecret.isEmpty()) {
                    secretToUse = fileSecret;
                }
            } catch (IOException e) {
                log.warn("JWT secret file not found: {}", jwtSecretFile);
            }
        }

        // Decode base64 secret and create key
        if (secretToUse != null && !secretToUse.isEmpty()) {
            try {
                byte[] decodedKey = Base64.getDecoder().decode(secretToUse);
                secretKey = Keys.hmacShaKeyFor(decodedKey);
                log.info("SecretKey initialized successfully from file, key size={} bits", decodedKey.length * 8);
            } catch (IllegalArgumentException e) {
                log.error("Invalid base64 format for JWT secret: {}", e.getMessage());
                throw new RuntimeException("Invalid JWT secret format", e);
            }
        } else {
            // Generate a new secure key if no secret file provided
            secretKey = Jwts.SIG.HS512.key().build();
            log.warn("Generated new secure JWT key (HS512)");
        }

        return secretKey;
    }

    public String generateToken(String userID) {
        return generateToken(userID, null);
    }

    public String generateToken(String userName, String email) {
        var token = Jwts.builder()
                .subject(userName)
                .expiration(new Date((new Date()).getTime() + tokenProperties.getAccessTokenExpiration() * 1000)) // Convert
                                                                                                                  // to
                                                                                                                  // ms
                .issuedAt(new Date())
                .signWith(readJwtSecret());
        if (email != null) {
            token.claim("email", email);
        }
        return token.compact();
    }

    public String generateRefreshToken(String userName) {
        return Jwts.builder()
                .subject(userName)
                .expiration(new Date((new Date()).getTime() + tokenProperties.getRefreshTokenExpiration() * 1000)) // Convert
                                                                                                                   // to
                                                                                                                   // ms
                .issuedAt(new Date())
                .claim("type", "refresh")
                .signWith(readJwtSecret())
                .compact();
    }

    public String getUserNameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claim = getClaimsFromToken(token);
            boolean isValid = userDetails.getUsername().equals(claim.getSubject()) && !isTokenExpired(token);

            boolean isActive = true;
            if (userDetails instanceof CustomUserDetailsService.CustomUserDetails customUserDetails) {
                isActive = customUserDetails.getUser().isActive();
            }

            return isValid && isActive;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public Boolean isTokenExpired(String token) {
        try {
            Claims claim = getClaimsFromToken(token);
            Date expiration = claim.getExpiration();
            if (expiration.before(new Date()))
                return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public UserDetails processOAuth2User(String registrationId, OAuth2User oAuth2User,
            OAuth2AuthorizedClient authorizedClient) {
        log.info("Processing OAuth2 user for provider: {}", registrationId);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Validate email
        if (email == null || email.trim().isEmpty()) {
            log.warn("OAuth2 user has no email attribute for provider: {}", registrationId);
            email = null; // Will use provider ID as fallback
        } else {
            email = email.trim();
        }

        if (name != null) {
            name = name.trim();
        }

        // Get provider-specific ID
        String providerId = getProviderSpecificId(registrationId, oAuth2User);
        if (providerId == null || providerId.isEmpty()) {
            log.error("Unable to extract provider-specific ID for provider: {}", registrationId);
            throw new IllegalArgumentException("Unable to extract provider ID from OAuth2 response");
        }

        UserEntity user = null;

        // Try to find existing user by OAuth2 ID first
        if ("google".equalsIgnoreCase(registrationId)) {
            user = userRepository.findByGoogleId(providerId).orElse(null);
        }

        // If not found by provider ID, try by email
        if (user == null && email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        // Create new user if not found
        if (user == null) {
            log.info("Creating new OAuth2 user for provider: {}, email: {}", registrationId, email);
            user = new UserEntity();
            user.setEmail(email);
            user.setFullName(name != null ? name : (email != null ? email.split("@")[0] : providerId));
            user.setUsername(generateUniqueUsername(email, registrationId));
            user.setPassword("");
            user.setProvider(registrationId);
            user.setActive(true);
            user.setEmailVerified(true);
            user.setRole(EnumRole.USER.getRoleName());

            if ("google".equalsIgnoreCase(registrationId)) {
                user.setGoogleId(providerId);
            }
        } else {
            // Update existing user
            log.info("Updating existing user: {}", user.getId());
            if (name != null && !name.isEmpty()) {
                user.setFullName(name);
            }
            if ("google".equalsIgnoreCase(registrationId)
                    && (user.getGoogleId() == null || user.getGoogleId().isEmpty())) {
                user.setGoogleId(providerId);
            }
        }

        // Update refresh token if available
        if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
            user.setRefreshToken(authorizedClient.getRefreshToken().getTokenValue());
        }

        user = userRepository.save(user);
        log.info("OAuth2 user processed successfully: {}", user.getUsername());

        return customUserDetailsService.loadUserByUsername(user.getUsername());
    }

    private String getProviderSpecificId(String registrationId, OAuth2User oAuth2User) {
        try {
            switch (registrationId.toLowerCase()) {
                case "google":
                    String sub = oAuth2User.getAttribute("sub");
                    if (sub != null && !sub.isEmpty()) {
                        return sub;
                    }
                    break;
                case "github":
                    Object githubId = oAuth2User.getAttribute("id");
                    if (githubId != null) {
                        return githubId.toString();
                    }
                    break;
                case "facebook":
                    Object fbId = oAuth2User.getAttribute("id");
                    if (fbId != null && !fbId.toString().isEmpty()) {
                        return fbId.toString();
                    }
                    break;
                default:
                    Object id = oAuth2User.getAttribute("id");
                    if (id != null && !id.toString().isEmpty()) {
                        return id.toString();
                    }
                    Object subDefault = oAuth2User.getAttribute("sub");
                    if (subDefault != null) {
                        return subDefault.toString();
                    }
            }
        } catch (Exception e) {
            log.error("Error extracting provider-specific ID for provider: {}", registrationId, e);
        }
        return null;
    }

    private String generateUniqueUsername(String email, String provider) {
        String baseUsername;
        if (email != null && !email.isEmpty()) {
            baseUsername = email.split("@")[0];
        } else {
            baseUsername = provider + "_user";
        }

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + provider + counter;
            counter++;
        }

        return username;
    }
}