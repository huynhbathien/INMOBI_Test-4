package com.mycompany.service.Impl;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.dto.request.ForgotPasswordRequestDTO;
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
import com.mycompany.service.AccessTokenStore;
import com.mycompany.service.AuthAccountService;
import com.mycompany.service.AuthSessionService;
import com.mycompany.service.OtpService;
import com.mycompany.service.RefreshTokenStore;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class AuthAccountServiceImpl implements AuthAccountService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuthSessionService authSessionService;
    private final AccessTokenStore accessTokenStore;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    @Transactional
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
        log.info("User '{}' registered awaiting email verification", username);

        otpService.generateAndSendOtp(email, OtpType.EMAIL_VERIFICATION);
        return Map.of("message", "Registration successful. OTP has been sent to " + maskEmail(email));
    }

    @Override
    @Transactional
    public Map<String, String> verifyEmailAndLogin(@Valid VerifyOtpRequestDTO dto) {
        otpService.verifyOtp(dto.getEmail(), dto.getOtpCode(), OtpType.EMAIL_VERIFICATION);

        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified for user '{}'", user.getUsername());

        return authSessionService.issueTokenPair(user.getUsername());
    }

    @Override
    public void resendOtp(@Valid ResendOtpRequestDTO dto) {
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        if (OtpType.EMAIL_VERIFICATION.equals(dto.getOtpType()) && user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already verified.");
        }

        otpService.generateAndSendOtp(dto.getEmail(), dto.getOtpType());
        log.info("OTP resent to '{}' for type={}", dto.getEmail(), dto.getOtpType());
    }

    @Override
    public void forgotPassword(@Valid ForgotPasswordRequestDTO dto) {
        userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        otpService.generateAndSendOtp(dto.getEmail(), OtpType.FORGOT_PASSWORD);
        log.info("Forgot-password OTP sent to '{}'", dto.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(@Valid ResetPasswordRequestDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    EnumAuthError.PASSWORD_MISMATCH.getMessage());
        }

        otpService.verifyOtp(dto.getEmail(), dto.getOtpCode(), OtpType.FORGOT_PASSWORD);

        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        accessTokenStore.deleteAccessToken(user.getUsername());
        refreshTokenStore.deleteRefreshToken(user.getUsername());
        log.info("Password reset successfully for user '{}'", user.getUsername());
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}