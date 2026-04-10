package com.mycompany.service.Impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.mycompany.entity.UserEntity;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.repository.UserRepository;
import com.mycompany.service.UserService;
import com.mycompany.service.UserSessionQueryService;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserSessionQueryService userSessionQueryService;

    public UserServiceImpl(UserRepository userRepository, UserSessionQueryService userSessionQueryService) {
        this.userRepository = userRepository;
        this.userSessionQueryService = userSessionQueryService;
    }

    @Override
    public Map<String, Object> getCurrentUserProfile(String username) {
        log.info("Fetching user profile for: {}", username);

        // Get user from database
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(EnumAuthError.USER_NOT_FOUND.getMessage()));

        // Build and return response
        return buildUserInfoMap(user);
    }

    @Override
    public Map<String, Object> getUserProfileById(Long userId) {
        log.info("Fetching user profile for userId: {}", userId);

        // Get user from database
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(EnumAuthError.USER_NOT_FOUND.getMessage()));

        // Build and return response
        return buildUserInfoMap(user);
    }

    /**
     * Build user info map from UserEntity
     * 
     * @param user UserEntity
     * @return User info map
     */
    private Map<String, Object> buildUserInfoMap(UserEntity user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("role", user.getRole());
        userInfo.put("active", user.isActive());
        userInfo.put("provider", user.getProvider());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("updatedAt", user.getUpdatedAt());

        // Get session info from Redis
        String sessionInfo = userSessionQueryService.getUserSessionInfo(user.getUsername());
        if (sessionInfo != null) {
            userInfo.put("sessionInfo", sessionInfo);
        }

        return userInfo;
    }
}
