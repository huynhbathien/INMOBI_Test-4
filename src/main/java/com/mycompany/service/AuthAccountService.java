package com.mycompany.service;

import java.util.Map;

import com.mycompany.dto.request.ForgotPasswordRequestDTO;
import com.mycompany.dto.request.RegisterRequestDTO;
import com.mycompany.dto.request.ResendOtpRequestDTO;
import com.mycompany.dto.request.ResetPasswordRequestDTO;
import com.mycompany.dto.request.VerifyOtpRequestDTO;

public interface AuthAccountService {

    Map<String, String> register(RegisterRequestDTO dto);

    Map<String, String> verifyEmailAndLogin(VerifyOtpRequestDTO dto);

    void resendOtp(ResendOtpRequestDTO dto);

    void forgotPassword(ForgotPasswordRequestDTO dto);

    void resetPassword(ResetPasswordRequestDTO dto);
}