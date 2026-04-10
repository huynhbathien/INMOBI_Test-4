package com.mycompany.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.dto.APIResponse;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.enums.EnumSuccess;
import com.mycompany.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/user")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current authenticated user profile
     * 
     * @return User profile information
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public APIResponse<Object> getCurrentUser() {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return APIResponse.error(EnumAuthError.UNAUTHORIZED.getCode(),
                        EnumAuthError.UNAUTHORIZED.getMessage(),
                        EnumAuthError.UNAUTHORIZED.name());
            }

            String username = authentication.getName();

            // Delegate to service for business logic
            return APIResponse.success(EnumSuccess.SUCCESS.getCode(),
                    "User profile retrieved successfully",
                    userService.getCurrentUserProfile(username));
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage(), e);
            return APIResponse.error(EnumAuthError.INTERNAL_ERROR.getCode(),
                    EnumAuthError.INTERNAL_ERROR.getMessage(),
                    e.getMessage());
        }
    }

    /**
     * Get user by ID (for admin purposes)
     * 
     * @param userId User ID
     * @return User information
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public APIResponse<Object> getUserById(@PathVariable Long userId) {
        try {
            // Delegate to service for business logic
            return APIResponse.success(EnumSuccess.SUCCESS.getCode(),
                    "User profile retrieved successfully",
                    userService.getUserProfileById(userId));
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage(), e);
            return APIResponse.error(EnumAuthError.INTERNAL_ERROR.getCode(),
                    EnumAuthError.INTERNAL_ERROR.getMessage(),
                    e.getMessage());
        }
    }
}
