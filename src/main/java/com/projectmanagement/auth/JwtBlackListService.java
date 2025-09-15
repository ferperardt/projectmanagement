package com.projectmanagement.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtBlackListService {

    private final JwtBlackListProperties properties;
    private final RedisTemplate<String, String> redisTemplate;

    public void blacklistToken(String jti, long expirationTime) {
        if (jti == null || jti.trim().isEmpty()) {
            log.warn("Attempted to blacklist token with null or empty JTI");
            return;
        }

        String key = buildKey(jti);
        long ttlSeconds = calculateTtl(expirationTime);

        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
            log.debug("Token blacklisted in Redis: JTI={}, TTL={}s", jti, ttlSeconds);
        } else {
            log.warn("Token already expired, not adding to blacklist: JTI={}", jti);
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return false;
        }

        String key = buildKey(jti);
        Boolean exists = redisTemplate.hasKey(key);
        boolean isBlacklisted = Boolean.TRUE.equals(exists);

        log.debug("Blacklist check: JTI={}, isBlacklisted={}", jti, isBlacklisted);
        return isBlacklisted;
    }

    public int getBlacklistedTokensCount() {
        String pattern = properties.getKeyPrefix() + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys.size() : 0;
    }

    public void clearAll() {
        String pattern = properties.getKeyPrefix() + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared all blacklisted tokens from Redis. Removed {} tokens", keys.size());
        } else {
            log.info("No blacklisted tokens found to clear");
        }
    }

    private String buildKey(String jti) {
        return properties.getKeyPrefix() + ":" + jti;
    }

    private long calculateTtl(long expirationTime) {
        long currentTime = System.currentTimeMillis();
        long ttlMillis = expirationTime - currentTime + (properties.getTtlBufferSeconds() * 1000L);
        return Math.max(0, ttlMillis / 1000);
    }
}