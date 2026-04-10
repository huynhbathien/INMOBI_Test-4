package com.mycompany.service.Impl;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.dto.request.ForgotPasswordRequestDTO;
import com.mycompany.dto.request.LoginRequestDTO;
import com.mycompany.dto.request.RegisterRequestDTO;
import com.mycompany.dto.request.ResendOtpRequestDTO;
import com.mycompany.dto.request.ResetPasswordRequestDTO;
import com.mycompany.dto.request.VerifyOtpRequestDTO;
import com.mycompany.entity.UserEntity;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.enums.EnumRole;
import com.mycompany.enums.OtpType;
import com.mycompany.mapstruct.UserMapper;
import com.mycompany.repository.UserRepository;
import com.mycompany.security.JwtUtils;
import com.mycompany.security.LoginAttemptService;
import com.mycompany.service.AuthService;
import com.mycompany.service.OtpService;
import com.mycompany.service.TokenRedisService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Validated
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final TokenRedisService tokenRedisService;
    private final LoginAttemptService loginAttemptService;
    private final OtpService otpService;

    @Override
    public Map<String, String> login(@Valid LoginRequestDTO dto, String clientIp) {
        if (loginAttemptService.isBlocked(clientIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    EnumAuthError.TOO_MANY_REQUESTS.getMessage());
        }

        String username = dto.getUsername();
        UserEntity user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(clientIp);
            int remaining = loginAttemptService.getRemainingAttempts(clientIp);
            log.warn("Invalid login for user '{}' from IP '{}'. Remaining attempts: {}", username, clientIp, remaining);
            throw new BadCredentialsException(EnumAuthError.INVALID_CREDENTIALS.getMessage());
        }

        ensureUserIsActive(user);

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    EnumAuthError.EMAIL_NOT_VERIFIED.getMessage());
        }

        loginAttemptService.loginSucceeded(clientIp);
        return issueTokenPair(user.getUsername(), user.getId());
    }

    @Override
    public Map<String, String> register(@Valid RegisterRequestDTO dto) {
        String username = dto.getUsername();
        String email = dto.getEmail();

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    EnumAuthError.USER_ALREADY_EXISTS.getMessage());
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    EnumAuthError.EMAIL_ALREADY_EXISTS.getMessage());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    EnumAuthError.PASSWORD_MISMATCH.getMessage());
        }

        UserEntity user = userMapper.toUserEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(EnumRole.USER.getRoleName());
        user.setEmailVerified(false);
        userRepository.save(user);

        otpService.generateAndSendOtp(email, OtpType.EMAIL_VERIFICATION);
        return Map.of("message", "Registration successful. OTP has been sent to " + maskEmail(email));
    }

    @Override
    public Map<String, String> verifyEmailAndLogin(@Valid VerifyOtpRequestDTO dto) {
        otpService.verifyOtp(dto.getEmail(), dto.getOtpCode(), OtpType.EMAIL_VERIFICATION);

        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        ensureUserIsActive(user);

        user.setEmailVerified(true);
        userRepository.save(user);
        return issueTokenPair(user.getUsername(), user.getId());
    }

    @Override
    public void resendOtp(@Valid ResendOtpRequestDTO dto) {
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        ensureUserIsActive(user);

        if (OtpType.EMAIL_VERIFICATION.equals(dto.getOtpType()) && user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already verified.");
        }

        otpService.generateAndSendOtp(dto.getEmail(), dto.getOtpType());
    }

    @Override
    public void forgotPassword(@Valid ForgotPasswordRequestDTO dto) {
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        ensureUserIsActive(user);

        otpService.generateAndSendOtp(dto.getEmail(), OtpType.FORGOT_PASSWORD);
    }

    @Override
    public void resetPassword(@Valid ResetPasswordRequestDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    EnumAuthError.PASSWORD_MISMATCH.getMessage());
        }

        otpService.verifyOtp(dto.getEmail(), dto.getOtpCode(), OtpType.FORGOT_PASSWORD);

        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        ensureUserIsActive(user);

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        tokenRedisService.deleteAccessToken(user.getUsername());
        tokenRedisService.deleteRefreshToken(user.getUsername());
    }

    @Override
    public Map<String, String> refreshToken(String clientRefreshToken) {
        if (jwtUtils.isTokenExpired(clientRefreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.REFRESH_TOKEN_EXPIRED.getMessage());
        }

        String username = jwtUtils.getUserNameFromToken(clientRefreshToken);
        String storedRefreshToken = tokenRedisService.getRefreshToken(username);
        if (storedRefreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }
        if (!storedRefreshToken.equals(clientRefreshToken)) {
            tokenRedisService.deleteRefreshToken(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));
        ensureUserIsActive(user);

        Long userId = tokenRedisService.getUserId(username);
        String newAccessToken = jwtUtils.generateToken(username);
        String newRefreshToken = jwtUtils.generateRefreshToken(username);
        tokenRedisService.saveAccessToken(username, userId, newAccessToken);
        tokenRedisService.saveRefreshToken(username, userId, newRefreshToken);

        return Map.of(
                "token", newAccessToken,
                "refreshToken", newRefreshToken);
    }

    @Override
    public void logout(String username) {
        tokenRedisService.deleteAccessToken(username);
        tokenRedisService.deleteRefreshToken(username);
    }

    private Map<String, String> issueTokenPair(String username, Long userId) {
        String accessToken = jwtUtils.generateToken(username);
        String refreshToken = jwtUtils.generateRefreshToken(username);
        tokenRedisService.saveAccessToken(username, userId, accessToken);
        tokenRedisService.saveRefreshToken(username, userId, refreshToken);
        return Map.of(
                "token", accessToken,
                "refreshToken", refreshToken);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private void ensureUserIsActive(UserEntity user) {
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    EnumAuthError.ACCOUNT_DISABLED.getMessage());
        }
    }
}
