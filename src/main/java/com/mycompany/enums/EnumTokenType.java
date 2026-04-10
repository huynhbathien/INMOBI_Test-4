package com.mycompany.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum EnumTokenType {
    ACCESS_TOKEN("ACCESS", "Access Token", 3600000L), // 1 hour
    REFRESH_TOKEN("REFRESH", "Refresh Token", 604800000L), // 7 days
    RESET_PASSWORD_TOKEN("RESET_PASSWORD", "Reset Password Token", 1800000L), // 30 minutes
    EMAIL_VERIFICATION_TOKEN("EMAIL_VERIFY", "Email Verification Token", 86400000L); // 24 hours

    String type;
    String description;
    Long expirationMs;

    EnumTokenType(String type, String description, Long expirationMs) {
        this.type = type;
        this.description = description;
        this.expirationMs = expirationMs;
    }

    public static EnumTokenType fromType(String type) {
        for (EnumTokenType tokenType : EnumTokenType.values()) {
            if (tokenType.type.equalsIgnoreCase(type)) {
                return tokenType;
            }
        }
        throw new IllegalArgumentException("Invalid token type: " + type);
    }
}
