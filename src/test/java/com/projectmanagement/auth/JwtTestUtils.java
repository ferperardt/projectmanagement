package com.projectmanagement.auth;

import com.projectmanagement.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.data.redis.core.RedisTemplate;

import javax.crypto.SecretKey;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public final class JwtTestUtils {

    private JwtTestUtils() {
        // Utility class
    }

    public static void cleanRedis(RedisTemplate<String, String> redisTemplate) {
        cleanRedisKeyPattern(redisTemplate, "refresh_token:*");
        cleanRedisKeyPattern(redisTemplate, "refresh_blacklist:*");
        cleanRedisKeyPattern(redisTemplate, "jwt:blacklist:*");
    }

    public static void cleanRedisKeyPattern(RedisTemplate<String, String> redisTemplate, String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public static String extractJtiFromToken(String token, JwtProperties jwtProperties, boolean isRefreshToken) {
        SecretKey key = isRefreshToken
                ? Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes())
                : Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getId();
    }

    public static String extractJtiFromRefreshToken(String refreshToken, JwtProperties jwtProperties) {
        return extractJtiFromToken(refreshToken, jwtProperties, true);
    }

    public static String extractJtiFromAccessToken(String accessToken, JwtProperties jwtProperties) {
        return extractJtiFromToken(accessToken, jwtProperties, false);
    }

    public static boolean hasRedisKey(RedisTemplate<String, String> redisTemplate, String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public static boolean isRefreshTokenInRedis(RedisTemplate<String, String> redisTemplate, String jti) {
        return hasRedisKey(redisTemplate, "refresh_token:" + jti);
    }

    public static boolean isRefreshTokenBlacklisted(RedisTemplate<String, String> redisTemplate, String jti) {
        return hasRedisKey(redisTemplate, "refresh_blacklist:" + jti);
    }

    public static boolean isAccessTokenBlacklisted(RedisTemplate<String, String> redisTemplate, String jti) {
        return hasRedisKey(redisTemplate, "jwt:blacklist:" + jti);
    }

    public static void verifyTokenRotation(RedisTemplate<String, String> redisTemplate,
                                           JwtProperties jwtProperties,
                                           String oldRefreshToken,
                                           String newRefreshToken) {
        String oldJti = extractJtiFromRefreshToken(oldRefreshToken, jwtProperties);
        String newJti = extractJtiFromRefreshToken(newRefreshToken, jwtProperties);

        assertThat(isRefreshTokenBlacklisted(redisTemplate, oldJti))
                .as("Old refresh token should be blacklisted")
                .isTrue();

        assertThat(isRefreshTokenInRedis(redisTemplate, newJti))
                .as("New refresh token should be stored in Redis")
                .isTrue();

        assertThat(newRefreshToken)
                .as("New refresh token should be different from old one")
                .isNotEqualTo(oldRefreshToken);
    }
}