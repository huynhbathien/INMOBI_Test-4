package com.mycompany.service;

import com.mycompany.enums.OtpType;

public interface OtpService {
    void generateAndSendOtp(String email, OtpType otpType);

    void verifyOtp(String email, String otpCode, OtpType otpType);
}
