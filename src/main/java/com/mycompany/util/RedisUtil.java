package com.mycompany.util;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Generic Redis Utility for common Redis operations
 * Supports operations such as set, get, delete, exists, increment, etc.
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Store data in Redis with TTL (Time To Live)
     * 
     * @param key     Redis key
     * @param value   value
     * @param timeout TTL (lifetime)
     * @param unit    time unit (SECONDS, MINUTES, HOURS, DAYS)
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Redis SET: key={}, timeout={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Error setting value in Redis for key: {}", key, e);
            throw new RuntimeException("Failed to set value in Redis", e);
        }
    }

    /**
     * Store data in Redis without TTL
     * 
     * @param key   Redis key
     * @param value value
     */
    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Redis SET: key={}", key);
        } catch (Exception e) {
            log.error("Error setting value in Redis for key: {}", key, e);
            throw new RuntimeException("Failed to set value in Redis", e);
        }
    }

    /**
     * Get data from Redis
     * 
     * @param key Redis key
     * @return value or null if not found
     */
    public String get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Redis GET: key={}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Error getting value from Redis for key: {}", key, e);
            return null;
        }
    }

    /**
     * Delete data from Redis
     * 
     * @param key Redis key
     * @return true if deleted successfully, false if key does not exist
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (deleted != null && deleted) {
                log.debug("Redis DELETE: key={}", key);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting value from Redis for key: {}", key, e);
            return false;
        }
    }

    /**
     * Delete multiple keys
     * 
     * @param keys list of Redis keys
     * @return number of deleted keys
     */
    public long delete(String... keys) {
        try {
            long count = redisTemplate.delete(Arrays.asList(keys));
            if (count > 0) {
                log.debug("Redis DELETE: {} keys deleted", count);
            }
            return count;
        } catch (Exception e) {
            log.error("Error deleting multiple keys from Redis", e);
            return 0;
        }
    }

    /**
     * Check whether a key exists
     * 
     * @param key Redis key
     * @return true if exists
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Error checking key existence in Redis for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get TTL of a key (in seconds)
     * 
     * @param key Redis key
     * @return remaining seconds, -1 if key has no TTL, -2 if key does not exist
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -2;
        } catch (Exception e) {
            log.error("Error getting expire time from Redis for key: {}", key, e);
            return -2;
        }
    }

    /**
     * Set TTL for a key
     * 
     * @param key     Redis key
     * @param timeout TTL (lifetime)
     * @param unit    time unit
     * @return true if set successfully
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            if (result != null && result) {
                log.debug("Redis EXPIRE: key={}, timeout={} {}", key, timeout, unit);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error setting expire time in Redis for key: {}", key, e);
            return false;
        }
    }

    /**
     * Increment value of a key (for counters)
     * 
     * @param key Redis key
     * @return value after increment
     */
    public long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            log.debug("Redis INCREMENT: key={}, value={}", key, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Error incrementing value in Redis for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Increment value of a key by a specific delta
     * 
     * @param key   Redis key
     * @param delta increment delta
     * @return value after increment
     */
    public long increment(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Redis INCREMENT: key={}, delta={}, value={}", key, delta, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Error incrementing value in Redis for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Decrement value of a key
     * 
     * @param key Redis key
     * @return value after decrement
     */
    public long decrement(String key) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key);
            log.debug("Redis DECREMENT: key={}, value={}", key, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Error decrementing value in Redis for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Store hash field in Redis
     * 
     * @param key     Redis key
     * @param hashKey hash field name
     * @param value   value
     */
    public void hset(String key, String hashKey, String value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            log.debug("Redis HSET: key={}, hashKey={}", key, hashKey);
        } catch (Exception e) {
            log.error("Error setting hash value in Redis for key: {}", key, e);
            throw new RuntimeException("Failed to set hash value in Redis", e);
        }
    }

    /**
     * Get hash field from Redis
     * 
     * @param key     Redis key
     * @param hashKey hash field name
     * @return value
     */
    public Object hget(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.error("Error getting hash value from Redis for key: {}", key, e);
            return null;
        }
    }

    /**
     * Delete hash field from Redis
     * 
     * @param key     Redis key
     * @param hashKey hash field name
     */
    public void hdel(String key, String hashKey) {
        try {
            redisTemplate.opsForHash().delete(key, hashKey);
            log.debug("Redis HDEL: key={}, hashKey={}", key, hashKey);
        } catch (Exception e) {
            log.error("Error deleting hash value from Redis for key: {}", key, e);
        }
    }

    /**
     * Flush all Redis data (CAUTION!)
     */
    public void flushDb() {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.serverCommands().flushDb();
                return null;
            });
            log.warn("Redis database flushed!");
        } catch (Exception e) {
            log.error("Error flushing Redis database", e);
        }
    }
}
