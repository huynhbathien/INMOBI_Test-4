package com.mycompany.service;

import java.util.Map;

public interface UserService {
    /**
     * Get current authenticated user profile
     * 
     * @param username Username
     * @return User profile information
     */
    Map<String, Object> getCurrentUserProfile(String username);

    /**
     * Get user profile by ID
     * 
     * @param userId User ID
     * @return User profile information
     */
    Map<String, Object> getUserProfileById(Long userId);
}
