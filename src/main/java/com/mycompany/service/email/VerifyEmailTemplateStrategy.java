package com.mycompany.service.email;

import org.springframework.stereotype.Component;

import com.mycompany.enums.OtpType;

@Component
public class VerifyEmailTemplateStrategy implements OtpEmailTemplateStrategy {

  @Override
  public OtpType supportedType() {
    return OtpType.EMAIL_VERIFICATION;
  }

  @Override
  public String buildSubject() {
    return "Verify Your Email - GameApp";
  }

  @Override
  public String buildHtml(String otpCode, int expiryMinutes) {
    return """
        <!DOCTYPE html>
        <html>
        <head><meta charset=\"UTF-8\"></head>
        <body style=\"margin:0;padding:0;font-family:Arial,Helvetica,sans-serif;background:#f4f7f6;\">
          <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f7f6;padding:40px 0;\">
            <tr><td align=\"center\">
              <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);\">
                <tr><td style=\"background:#4F46E5;padding:32px 40px;text-align:center;\">
                  <h1 style=\"margin:0;color:#ffffff;font-size:24px;font-weight:700;\">GameApp</h1>
                </td></tr>
                <tr><td style=\"padding:40px;\">
                  <h2 style=\"margin:0 0 16px;color:#1a1a2e;font-size:20px;\">Verify Your Email Address</h2>
                  <p style=\"margin:0 0 24px;color:#555;line-height:1.6;\">
                    Thank you for registering! Use the code below to verify your email address.
                    The code is valid for <strong>%d minutes</strong>.
                  </p>
                  <div style=\"background:#f4f7f6;border:2px dashed #4F46E5;border-radius:8px;padding:24px;text-align:center;margin:0 0 24px;\">
                    <span style=\"font-size:36px;font-weight:700;letter-spacing:12px;color:#4F46E5;\">%s</span>
                  </div>
                  <p style=\"margin:0;color:#999;font-size:13px;\">
                    If you did not create an account, please ignore this email.
                  </p>
                </td></tr>
                <tr><td style=\"background:#f4f7f6;padding:20px 40px;text-align:center;\">
                  <p style=\"margin:0;color:#aaa;font-size:12px;\">&copy; 2026 GameApp. All rights reserved.</p>
                </td></tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """
        .formatted(expiryMinutes, otpCode);
  }
}
