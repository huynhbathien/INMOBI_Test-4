package com.mycompany.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum EnumRole {
    ADMIN("ROLE_ADMIN", "Administrator"),
    USER("ROLE_USER", "User");

    String roleName;
    String description;

    EnumRole(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public static EnumRole fromRoleName(String roleName) {
        for (EnumRole role : EnumRole.values()) {
            if (role.roleName.equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role name: " + roleName);
    }
}
