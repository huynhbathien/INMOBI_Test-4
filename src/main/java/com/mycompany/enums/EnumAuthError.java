package com.mycompany.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum EnumAuthError {
    // Authentication errors
    INVALID_CREDENTIALS(1001, "Invalid username or password"),
    ACCOUNT_DISABLED(1002, "Account is disabled"),
    ACCOUNT_LOCKED(1003, "Account is locked"),
    ACCOUNT_EXPIRED(1004, "Account has expired"),

    // Token errors
    TOKEN_EXPIRED(2001, "Token has expired"),
    TOKEN_INVALID(2002, "Invalid token"),
    TOKEN_MISSING(2003, "Token is missing"),
    TOKEN_MALFORMED(2004, "Malformed token"),
    TOKEN_REVOKED(2007, "Token has been revoked"),
    REFRESH_TOKEN_NOT_FOUND(2005, "Refresh token not found or expired"),
    REFRESH_TOKEN_EXPIRED(2006, "Refresh token has expired"),

    // Authorization errors
    INSUFFICIENT_PERMISSIONS(3001, "Insufficient permissions"),
    ACCESS_DENIED(3002, "Access denied"),
    ROLE_NOT_FOUND(3003, "Role not found"),

    // User errors
    USER_NOT_FOUND(4001, "User not found"),
    USER_ALREADY_EXISTS(4002, "User already exists"),
    EMAIL_ALREADY_EXISTS(4003, "Email already exists"),
    USERNAME_ALREADY_EXISTS(4004, "Username already exists"),

    // Password errors
    PASSWORD_WEAK(5001, "Password is too weak"),
    PASSWORD_MISMATCH(5002, "Passwords do not match"),
    INVALID_OLD_PASSWORD(5003, "Invalid old password"),

    // Session errors
    SESSION_EXPIRED(6001, "Session has expired"),
    SESSION_INVALID(6002, "Invalid session"),
    MAX_SESSIONS_EXCEEDED(6003, "Maximum number of sessions exceeded"),

    // OAuth2 errors
    OAUTH2_FAILURE(7001, "OAuth2 authentication failed"),
    OAUTH2_INVALID_PROVIDER(7002, "Invalid OAuth2 provider"),
    OAUTH2_USER_EXTRACTION_FAILED(7003, "Failed to extract user information from OAuth2"),

    // OTP errors
    OTP_INVALID(8001, "Invalid OTP code"),
    OTP_EXPIRED(8002, "OTP has expired. Please request a new one."),
    OTP_NOT_FOUND(8003, "OTP not found or already used"),
    EMAIL_NOT_VERIFIED(8004, "Email address is not verified. Please check your email for the OTP."),

    // General errors
    UNAUTHORIZED(9001, "Unauthorized access"),
    FORBIDDEN(9002, "Forbidden"),
    TOO_MANY_REQUESTS(9003, "Too many requests. Please try again later."),
    INTERNAL_ERROR(9999, "Internal server error");

    int code;
    String message;

    EnumAuthError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static EnumAuthError fromCode(int code) {
        for (EnumAuthError error : EnumAuthError.values()) {
            if (error.code == code) {
                return error;
            }
        }
        throw new IllegalArgumentException("Invalid error code: " + code);
    }
}
