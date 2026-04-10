package com.mycompany.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum EnumSuccess {

    SUCCESS(200, "Success"),

    // Authentication Successes Enums
    LOGIN_SUCCESS(201, "Login successful"),
    REGISTRATION_SUCCESS(202, "Registration successful. Please verify your email."),
    EMAIL_VERIFIED(203, "Email verified successfully"),
    OTP_SENT(204, "OTP sent successfully"),
    PASSWORD_RESET_SUCCESS(205, "Password reset successfully"),

    // Admin Successes Enums
    ADMIN_USER_STATUS_UPDATED(400, "User status updated successfully"),
    ADMIN_USER_ROLE_UPDATED(401, "User role updated successfully"),
    ;

    String message;
    int code;

    EnumSuccess(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static EnumSuccess fromCode(int code) {
        for (EnumSuccess success : EnumSuccess.values()) {
            if (success.code == code) {
                return success;
            }
        }
        throw new IllegalArgumentException("Invalid success code: " + code);
    }

}
