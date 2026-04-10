package com.mycompany.service.Impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.enums.OtpType;
import com.mycompany.service.EmailService;
import com.mycompany.service.EmailSenderService;
import com.mycompany.service.email.OtpEmailTemplateStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

  private final EmailSenderService emailSenderService;
  private final Map<OtpType, OtpEmailTemplateStrategy> strategyMap;

  public EmailServiceImpl(EmailSenderService emailSenderService, List<OtpEmailTemplateStrategy> strategyList) {
    this.emailSenderService = emailSenderService;
    this.strategyMap = strategyList.stream()
        .collect(Collectors.toMap(OtpEmailTemplateStrategy::supportedType, s -> s));
  }

  @Value("${otp.expiry-minutes:10}")
  private int otpExpiryMinutes;

  @Override
  public void sendOtpEmail(String to, String otpCode, OtpType otpType) {
    OtpEmailTemplateStrategy templateStrategy = resolveTemplateStrategy(otpType);
    emailSenderService.sendHtmlEmail(
        to,
        templateStrategy.buildSubject(),
        templateStrategy.buildHtml(otpCode, otpExpiryMinutes));
    log.info("OTP email ({}) sent to {}", otpType, to);
  }

  private OtpEmailTemplateStrategy resolveTemplateStrategy(OtpType otpType) {
    if (otpType == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP type is required.");
    }

    OtpEmailTemplateStrategy strategy = strategyMap.get(otpType);
    if (strategy == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported OTP type: " + otpType);
    }
    return strategy;
  }
}
