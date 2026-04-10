package com.mycompany.service.email;

import com.mycompany.enums.OtpType;

public interface OtpEmailTemplateStrategy {
  OtpType supportedType();

  String buildSubject();

  String buildHtml(String otpCode, int expiryMinutes);
}
