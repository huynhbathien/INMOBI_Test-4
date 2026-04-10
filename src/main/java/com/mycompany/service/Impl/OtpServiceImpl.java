package com.mycompany.service.Impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.entity.OtpEntity;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.enums.OtpType;
import com.mycompany.repository.OtpRepository;
import com.mycompany.service.EmailService;
import com.mycompany.service.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void generateAndSendOtp(String email, OtpType otpType) {
        // Remove all previous OTPs for this email + type before issuing a new one
        otpRepository.deleteByEmailAndOtpType(email, otpType);

        String otpCode = generateOtpCode();

        OtpEntity otp = new OtpEntity();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setOtpType(otpType);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        otpRepository.save(otp);
        log.info("OTP generated for email={} type={}", email, otpType);

        emailService.sendOtpEmail(email, otpCode, otpType);
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String otpCode, OtpType otpType) {
        OtpEntity otp = otpRepository
                .findTopByEmailAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(email, otpType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        EnumAuthError.OTP_NOT_FOUND.getMessage()));

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otpRepository.delete(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    EnumAuthError.OTP_EXPIRED.getMessage());
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    EnumAuthError.OTP_INVALID.getMessage());
        }

        otp.setUsed(true);
        otpRepository.save(otp);
        log.info("OTP verified for email={} type={}", email, otpType);
    }

    private String generateOtpCode() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }
}
