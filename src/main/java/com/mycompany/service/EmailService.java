package com.mycompany.service;

import com.mycompany.enums.OtpType;

public interface EmailService {
    void sendOtpEmail(String to, String otpCode, OtpType otpType);
}
