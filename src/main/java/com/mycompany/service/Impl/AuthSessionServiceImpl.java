package com.mycompany.service.Impl;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.dto.request.LoginRequestDTO;
import com.mycompany.entity.UserEntity;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.repository.UserRepository;
import com.mycompany.security.JwtUtils;
import com.mycompany.security.LoginAttemptService;
import com.mycompany.service.AccessTokenStore;
import com.mycompany.service.AuthSessionService;
import com.mycompany.service.RefreshTokenStore;
import com.mycompany.service.UserSessionQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class AuthSessionServiceImpl implements AuthSessionService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AccessTokenStore accessTokenStore;
    private final RefreshTokenStore refreshTokenStore;
    private final UserSessionQueryService userSessionQueryService;
    private final LoginAttemptService loginAttemptService;

    @Override
    public Map<String, String> login(@Valid LoginRequestDTO dto, String clientIp) {
        if (loginAttemptService.isBlocked(clientIp)) {
            log.warn("Blocked login attempt from IP: {}", clientIp);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    EnumAuthError.TOO_MANY_REQUESTS.getMessage());
        }

        String username = dto.getUsername();
        UserEntity user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(clientIp);
            int remaining = loginAttemptService.getRemainingAttempts(clientIp);
            log.warn("Invalid credentials for user '{}' from IP '{}'. Remaining attempts: {}",
                    username, clientIp, remaining);
            throw new BadCredentialsException(EnumAuthError.INVALID_CREDENTIALS.getMessage());
        }

        if (!user.isEmailVerified()) {
            log.warn("Login attempt for unverified email by user '{}'", username);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    EnumAuthError.EMAIL_NOT_VERIFIED.getMessage());
        }

        loginAttemptService.loginSucceeded(clientIp);
        return issueTokenPair(user.getUsername());
    }

    @Override
    public Map<String, String> issueTokenPair(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        String accessToken = jwtUtils.generateToken(user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUsername());
        accessTokenStore.saveAccessToken(user.getUsername(), user.getId(), accessToken);
        refreshTokenStore.saveRefreshToken(user.getUsername(), user.getId(), refreshToken);
        return Map.of(
                "token", accessToken,
                "refreshToken", refreshToken);
    }

    @Override
    public Map<String, String> refreshToken(String clientRefreshToken) {
        if (jwtUtils.isTokenExpired(clientRefreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.REFRESH_TOKEN_EXPIRED.getMessage());
        }

        String username = jwtUtils.getUserNameFromToken(clientRefreshToken);
        log.info("Refresh token request for user: {}", username);

        String storedRefreshToken = refreshTokenStore.getRefreshToken(username);
        if (storedRefreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }
        if (!storedRefreshToken.equals(clientRefreshToken)) {
            refreshTokenStore.deleteRefreshToken(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }

        Long userId = userSessionQueryService.getUserId(username);
        String newAccessToken = jwtUtils.generateToken(username);
        String newRefreshToken = jwtUtils.generateRefreshToken(username);
        accessTokenStore.saveAccessToken(username, userId, newAccessToken);
        refreshTokenStore.saveRefreshToken(username, userId, newRefreshToken);
        log.info("Tokens rotated for user: {}", username);

        return Map.of(
                "token", newAccessToken,
                "refreshToken", newRefreshToken);
    }

    @Override
    public void logout(String username) {
        accessTokenStore.deleteAccessToken(username);
        refreshTokenStore.deleteRefreshToken(username);
        log.info("Logged out user: {}", username);
    }
}