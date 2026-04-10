package com.mycompany.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mycompany.util.RedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Token Redis Service - Manages Access Tokens and Refresh Tokens in Redis
 * 
 * Refresh Token: 7-30 days, stored server-side so it can be revoked
 * Access Token: 15-30 minutes, typically stored on the client (FE), server-side
 * storage is optional
 * (optional)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisService
        implements RefreshTokenStore, AccessTokenStore, UserSessionQueryService, TokenBlacklistService {

    private final RedisUtil redisUtil;

    @Value("${token.access-token-expiration:1800}")
    private long accessTokenExpiration; // in seconds (15-30 minutes)

    @Value("${token.refresh-token-expiration:604800}")
    private long refreshTokenExpiration; // in seconds (7 days)

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String USER_SESSION_PREFIX = "user_session:";

    // ==================== REFRESH TOKEN OPERATIONS ====================

    /**
     * Store refresh token in Redis (with userId)
     * 
     * @param username     username/email
     * @param userId       user ID
     * @param refreshToken refresh token value
     */
    public void saveRefreshToken(String username, Long userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + username;
        try {
            // Store refresh token with TTL
            redisUtil.set(key, refreshToken, refreshTokenExpiration, TimeUnit.SECONDS);

            // Store userId for tracking
            String userKey = USER_SESSION_PREFIX + username;
            redisUtil.hset(userKey, "userId", String.valueOf(userId));
            redisUtil.hset(userKey, "refreshToken", refreshToken);
            redisUtil.hset(userKey, "loginTime", String.valueOf(System.currentTimeMillis()));
            redisUtil.expire(userKey, refreshTokenExpiration, TimeUnit.SECONDS);

            log.info("Refresh token saved for user: {} (userId: {})", username, userId);
        } catch (Exception e) {
            log.error("Error saving refresh token for user: {}", username, e);
            throw e;
        }
    }

    /**
     * Get refresh token from Redis
     * 
     * @param username username/email
     * @return refresh token or null
     */
    public String getRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        try {
            String token = redisUtil.get(key);
            if (token != null) {
                log.debug("Refresh token retrieved for user: {}", username);
            }
            return token;
        } catch (Exception e) {
            log.error("Error retrieving refresh token for user: {}", username, e);
            return null;
        }
    }

    /**
     * Validate refresh token (check expire)
     * 
     * @param username username/email
     * @return true if token is still valid
     */
    public boolean validateRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        try {
            return redisUtil.exists(key);
        } catch (Exception e) {
            log.error("Error validating refresh token for user: {}", username, e);
            return false;
        }
    }

    /**
     * Delete refresh token (logout)
     * 
     * @param username username/email
     */
    public void deleteRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        String userKey = USER_SESSION_PREFIX + username;
        try {
            redisUtil.delete(key);
            redisUtil.delete(userKey);
            log.info("Refresh token deleted for user: {}", username);
        } catch (Exception e) {
            log.error("Error deleting refresh token for user: {}", username, e);
        }
    }

    /**
     * Get refresh token TTL (remaining lifetime)
     * 
     * @param username username/email
     * @return remaining seconds
     */
    public long getRefreshTokenExpire(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        try {
            return redisUtil.getExpire(key);
        } catch (Exception e) {
            log.error("Error getting refresh token expiry for user: {}", username, e);
            return -2; // key not found
        }
    }

    // ==================== ACCESS TOKEN OPERATIONS (Optional - stored on client)
    // ====================

    /**
     * Store access token in Redis (optional - for tracking)
     * Typically FE stores access tokens, server-side storage is not required
     * 
     * @param username    username/email
     * @param userId      user ID
     * @param accessToken access token value
     */
    public void saveAccessToken(String username, Long userId, String accessToken) {
        String key = ACCESS_TOKEN_PREFIX + username;
        try {
            redisUtil.set(key, accessToken, accessTokenExpiration, TimeUnit.SECONDS);
            log.debug("Access token saved for user: {} (userId: {})", username, userId);
        } catch (Exception e) {
            log.error("Error saving access token for user: {}", username, e);
        }
    }

    /**
     * Get access token from Redis
     * 
     * @param username username/email
     * @return access token or null
     */
    public String getAccessToken(String username) {
        String key = ACCESS_TOKEN_PREFIX + username;
        try {
            return redisUtil.get(key);
        } catch (Exception e) {
            log.error("Error retrieving access token for user: {}", username, e);
            return null;
        }
    }

    /**
     * Delete access token
     * 
     * @param username username/email
     */
    public void deleteAccessToken(String username) {
        String key = ACCESS_TOKEN_PREFIX + username;
        try {
            redisUtil.delete(key);
            log.debug("Access token deleted for user: {}", username);
        } catch (Exception e) {
            log.error("Error deleting access token for user: {}", username, e);
        }
    }

    // ==================== USER SESSION OPERATIONS ====================

    /**
     * Get userId from user session
     * 
     * @param username username/email
     * @return userId or null
     */
    public Long getUserId(String username) {
        String userKey = USER_SESSION_PREFIX + username;
        try {
            Object userIdObj = redisUtil.hget(userKey, "userId");
            if (userIdObj != null) {
                return Long.parseLong(userIdObj.toString());
            }
        } catch (Exception e) {
            log.error("Error getting userId for user: {}", username, e);
        }
        return null;
    }

    /**
     * Get user session information
     * 
     * @param username username/email
     * @return userId
     */
    public String getUserSessionInfo(String username) {
        String userKey = USER_SESSION_PREFIX + username;
        try {
            Object userIdObj = redisUtil.hget(userKey, "userId");
            Object loginTimeObj = redisUtil.hget(userKey, "loginTime");

            if (userIdObj != null) {
                long loginTime = loginTimeObj != null ? Long.parseLong(loginTimeObj.toString()) : 0;
                long now = System.currentTimeMillis();
                long sessionDuration = (now - loginTime) / 1000; // in seconds

                return String.format("userId=%s, sessionDuration=%ds", userIdObj, sessionDuration);
            }
        } catch (Exception e) {
            log.error("Error getting user session info for user: {}", username, e);
        }
        return null;
    }

    /**
     * Delete entire user session
     * 
     * @param username username/email
     */
    public void deleteUserSession(String username) {
        String userKey = USER_SESSION_PREFIX + username;
        try {
            redisUtil.delete(userKey);
            log.debug("User session deleted for user: {}", username);
        } catch (Exception e) {
            log.error("Error deleting user session for user: {}", username, e);
        }
    }

    // ==================== BLACKLIST OPERATIONS (For Token Revocation)
    // ====================
    private static final String BLACKLIST_PREFIX = "token_blacklist:";

    /**
     * Add token to blacklist (revoke token)
     * Used when user logs out or changes password
     * 
     * @param token      token value
     * @param expiryTime token expiration time (ms)
     */
    public void addToBlacklist(String token, long expiryTime) {
        String key = BLACKLIST_PREFIX + token;
        try {
            long now = System.currentTimeMillis();
            long ttlSeconds = (expiryTime - now) / 1000;

            if (ttlSeconds > 0) {
                redisUtil.set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
                log.info("Token added to blacklist with TTL: {} seconds", ttlSeconds);
            }
        } catch (Exception e) {
            log.error("Error adding token to blacklist", e);
        }
    }

    /**
     * Check whether token is in blacklist
     * 
     * @param token token value
     * @return true if token is revoked
     */
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        try {
            return redisUtil.exists(key);
        } catch (Exception e) {
            log.error("Error checking token blacklist", e);
            return false;
        }
    }

    /**
     * Remove token from blacklist
     * 
     * @param token token value
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        try {
            redisUtil.delete(key);
            log.debug("Token removed from blacklist");
        } catch (Exception e) {
            log.error("Error removing token from blacklist", e);
        }
    }
}
