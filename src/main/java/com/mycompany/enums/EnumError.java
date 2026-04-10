package com.mycompany.enums;

import lombok.Getter;

@Getter
public enum EnumError {

    // Admin Error Enums
    USER_NOT_FOUND(401, "User not found"),
    INVALID_ROLE(402, "Invalid role specified");

    private int code;
    private String message;

    EnumError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static EnumError fromCode(int code) {
        for (EnumError error : EnumError.values()) {
            if (error.code == code) {
                return error;
            }
        }
        throw new IllegalArgumentException("Invalid error code: " + code);

    }
}
