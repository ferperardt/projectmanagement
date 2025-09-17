package com.projectmanagement.auth;

import com.projectmanagement.config.JwtProperties;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public String generateRefreshToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "refresh");
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("username", user.getUsername());

        String refreshToken = Jwts.builder()
                .claims(extraClaims)
                .subject(user.getEmail())
                .issuer(jwtProperties.getIssuer())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiration()))
                .signWith(getRefreshSigningKey())
                .compact();

        String jti = extractJti(refreshToken);
        storeRefreshToken(jti, user.getEmail());

        log.debug("Generated refresh token for user: {}", user.getEmail());
        return refreshToken;
    }

    public boolean isValidRefreshToken(String refreshToken) {
        try {
            String jti = extractJti(refreshToken);
            String email = extractEmail(refreshToken);

            if (isRefreshTokenBlacklisted(jti)) {
                log.warn("Refresh token is blacklisted: {}", jti);
                return false;
            }

            if (isTokenExpired(refreshToken)) {
                log.warn("Refresh token is expired for user: {}", email);
                return false;
            }

            String storedEmail = getStoredEmail(jti);
            if (!email.equals(storedEmail)) {
                log.warn("Refresh token email mismatch. Token email: {}, Stored email: {}", email, storedEmail);
                return false;
            }

            return true;
        } catch (JwtException e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    public User getUserFromRefreshToken(String refreshToken) {
        String email = extractEmail(refreshToken);
        return userService.findByEmail(email);
    }

    public void invalidateRefreshToken(String refreshToken) {
        String jti = extractJti(refreshToken);
        long expirationTime = extractExpiration(refreshToken).getTime();

        blacklistRefreshToken(jti, expirationTime);
        removeStoredToken(jti);

        log.debug("Invalidated refresh token: {}", jti);
    }

    private void storeRefreshToken(String jti, String email) {
        String key = REFRESH_TOKEN_PREFIX + jti;
        long ttlSeconds = jwtProperties.getRefreshExpiration() / 1000;
        redisTemplate.opsForValue().set(key, email, Duration.ofSeconds(ttlSeconds));
    }

    private String getStoredEmail(String jti) {
        String key = REFRESH_TOKEN_PREFIX + jti;
        return redisTemplate.opsForValue().get(key);
    }

    private void removeStoredToken(String jti) {
        String key = REFRESH_TOKEN_PREFIX + jti;
        redisTemplate.delete(key);
    }

    private void blacklistRefreshToken(String jti, long expirationTime) {
        String key = "refresh_blacklist:" + jti;
        long ttlSeconds = Math.max(0, (expirationTime - System.currentTimeMillis()) / 1000);

        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
        }
    }

    private boolean isRefreshTokenBlacklisted(String jti) {
        String key = "refresh_blacklist:" + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private SecretKey getRefreshSigningKey() {
        byte[] keyBytes = jwtProperties.getRefreshSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getRefreshSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}