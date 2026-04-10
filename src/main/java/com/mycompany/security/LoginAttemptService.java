package com.mycompany.security;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.mycompany.config.BruteForceProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Brute-force protection: tracks failed login attempts per IP/username.
 * After MAX_ATTEMPTS failures the key is blocked for BLOCK_DURATION_MINUTES.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final String ATTEMPT_PREFIX = "login_attempt:";
    private static final String BLOCKED_PREFIX = "login_blocked:";

    private final RedisTemplate<String, String> redisTemplate;
    private final BruteForceProperties bruteForceProperties;

    public LoginAttemptService(RedisTemplate<String, String> redisTemplate,
            BruteForceProperties bruteForceProperties) {
        this.redisTemplate = redisTemplate;
        this.bruteForceProperties = bruteForceProperties;
    }

    // -----------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------

    /** Call this after a successful login to reset the counter. */
    public void loginSucceeded(String key) {
        redisTemplate.delete(ATTEMPT_PREFIX + key);
        redisTemplate.delete(BLOCKED_PREFIX + key);
        log.debug("Login succeeded – attempt counter cleared for '{}'", key);
    }

    /**
     * Call this after every failed login attempt.
     *
     * @param key IP address or username
     */
    public void loginFailed(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;
        String blockedKey = BLOCKED_PREFIX + key;

        Long attempts = redisTemplate.opsForValue().increment(attemptKey);

        // Set / refresh TTL on the attempt counter (sliding window)
        redisTemplate.expire(attemptKey, bruteForceProperties.getBlockDurationMinutes(), TimeUnit.MINUTES);

        if (attempts >= bruteForceProperties.getMaxAttempts()) {
            redisTemplate.opsForValue().set(blockedKey, "blocked",
                    bruteForceProperties.getBlockDurationMinutes(), TimeUnit.MINUTES);
            log.warn("Key '{}' is now BLOCKED after {} failed attempts ({}min lock).",
                    key, attempts, bruteForceProperties.getBlockDurationMinutes());
        } else {
            log.warn("Failed login attempt #{} for key '{}'", attempts, key);
        }
    }

    /**
     * @return true if the key is currently blocked (too many failed attempts).
     */
    public boolean isBlocked(String key) {
        Boolean exists = redisTemplate.hasKey(BLOCKED_PREFIX + key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Returns the remaining number of allowed attempts (0 means already blocked).
     */
    public int getRemainingAttempts(String key) {
        String val = redisTemplate.opsForValue().get(ATTEMPT_PREFIX + key);
        if (val == null)
            return bruteForceProperties.getMaxAttempts();
        int used = Integer.parseInt(val);
        return Math.max(0, bruteForceProperties.getMaxAttempts() - used);
    }
}
