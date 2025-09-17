package com.projectmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.auth.dto.LoginRequest;
import com.projectmanagement.auth.dto.LoginResponse;
import com.projectmanagement.auth.dto.RefreshTokenRequest;
import com.projectmanagement.auth.dto.RefreshTokenResponse;
import com.projectmanagement.config.JwtProperties;
import com.projectmanagement.exception.dto.ErrorResponse;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserRepository;
import com.projectmanagement.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.SecretKey;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class RefreshTokenIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/auth";

        // Clean database and Redis
        userRepository.deleteAll();

        // Clean specific Redis keys instead of flushing entire database
        Set<String> refreshTokenKeys = redisTemplate.keys("refresh_token:*");
        Set<String> blacklistKeys = redisTemplate.keys("refresh_blacklist:*");

        if (!refreshTokenKeys.isEmpty()) {
            redisTemplate.delete(refreshTokenKeys);
        }
        if (!blacklistKeys.isEmpty()) {
            redisTemplate.delete(blacklistKeys);
        }

        // Create test user
        createAndSaveUser("dev@prjctmng.com", "dev", "prjctmng432!dev", UserRole.DEVELOPER);
    }

    private void createAndSaveUser(String email, String username, String password, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setTwoFactorEnabled(false);

        userRepository.save(user);
    }

    private LoginResponse loginAndGetTokens(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/login",
                loginRequest,
                LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    private ResponseEntity<RefreshTokenResponse> refreshToken(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                baseUrl + "/refresh",
                HttpMethod.POST,
                entity,
                RefreshTokenResponse.class
        );
    }

    private ResponseEntity<ErrorResponse> refreshTokenExpectingError(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                baseUrl + "/refresh",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
    }

    private String extractJtiFromRefreshToken(String refreshToken) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();
        return claims.getId();
    }

    private boolean isRefreshTokenInRedis(String jti) {
        String key = "refresh_token:" + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private boolean isRefreshTokenBlacklisted(String jti) {
        String key = "refresh_blacklist:" + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void verifyTokenRotation(String oldRefreshToken, String newRefreshToken) {
        String oldJti = extractJtiFromRefreshToken(oldRefreshToken);
        String newJti = extractJtiFromRefreshToken(newRefreshToken);

        // Verify old token is blacklisted
        assertThat(isRefreshTokenBlacklisted(oldJti))
                .as("Old refresh token should be blacklisted")
                .isTrue();

        // Verify new token is stored
        assertThat(isRefreshTokenInRedis(newJti))
                .as("New refresh token should be stored in Redis")
                .isTrue();

        // Verify tokens are different
        assertThat(newRefreshToken)
                .as("New refresh token should be different from old one")
                .isNotEqualTo(oldRefreshToken);
    }

    @Test
    @DisplayName("Given user has logged in, when refresh token is used, then new token pair should be generated and Redis should be updated")
    void givenUserLoggedIn_whenRefreshToken_thenNewTokenPairGeneratedAndRedisUpdated() {
        // Given user has logged in and received tokens
        LoginResponse loginResponse = loginAndGetTokens("dev@prjctmng.com", "prjctmng432!dev");
        String originalAccessToken = loginResponse.accessToken();
        String originalRefreshToken = loginResponse.refreshToken();

        // Verify original refresh token is stored in Redis
        String originalJti = extractJtiFromRefreshToken(originalRefreshToken);
        assertThat(isRefreshTokenInRedis(originalJti))
                .as("Original refresh token should be stored in Redis")
                .isTrue();

        // When refresh token is used
        ResponseEntity<RefreshTokenResponse> refreshResponse = refreshToken(originalRefreshToken);

        // Then the response should be successful
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();

        RefreshTokenResponse tokenResponse = refreshResponse.getBody();

        // And new tokens should be generated
        assertThat(tokenResponse.accessToken())
                .as("New access token should be generated")
                .isNotNull()
                .isNotEmpty()
                .startsWith("eyJ")
                .isNotEqualTo(originalAccessToken);

        assertThat(tokenResponse.refreshToken())
                .as("New refresh token should be generated")
                .isNotNull()
                .isNotEmpty()
                .startsWith("eyJ")
                .isNotEqualTo(originalRefreshToken);

        // And should have correct metadata
        assertThat(tokenResponse.tokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.expiresIn()).isEqualTo(jwtProperties.getExpiration() / 1000);

        // And Redis should be updated with token rotation
        verifyTokenRotation(originalRefreshToken, tokenResponse.refreshToken());
    }

    @Test
    @DisplayName("Given refresh token was used, when same token is used again, then should be rejected and remain blacklisted")
    void givenUsedRefreshToken_whenTryToReuseToken_thenShouldBeRejectedAndRemainBlacklisted() {
        // Given refresh token was used successfully
        LoginResponse loginResponse = loginAndGetTokens("dev@prjctmng.com", "prjctmng432!dev");
        String refreshToken = loginResponse.refreshToken();

        // First use should succeed
        ResponseEntity<RefreshTokenResponse> firstRefresh = refreshToken(refreshToken);
        assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        String jti = extractJtiFromRefreshToken(refreshToken);
        assertThat(isRefreshTokenBlacklisted(jti))
                .as("Token should be blacklisted after first use")
                .isTrue();

        // When same token is used again
        ResponseEntity<ErrorResponse> secondRefresh = refreshTokenExpectingError(refreshToken);

        // Then should be rejected
        assertThat(secondRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(secondRefresh.getBody()).isNotNull();
        assertThat(secondRefresh.getBody().message())
                .contains("Invalid or expired refresh token");

        // And token should remain blacklisted
        assertThat(isRefreshTokenBlacklisted(jti))
                .as("Token should remain blacklisted")
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Given blank refresh token, when try to refresh, then should get validation error")
    void givenBlankRefreshToken_whenTryToRefresh_thenShouldGetValidationError(String blankToken) {
        // When blank refresh token is used
        ResponseEntity<ErrorResponse> response = refreshTokenExpectingError(blankToken);

        // Then should get validation error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .containsAnyOf("Refresh token is required", "Request validation failed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-token", "malformed.jwt.token", "eyJhbGciOiJIUzI1NiJ9.invalid.signature"})
    @DisplayName("Given malformed refresh token, when try to refresh, then should be unauthorized")
    void givenMalformedRefreshToken_whenTryToRefresh_thenShouldBeUnauthorized(String malformedToken) {
        // When malformed refresh token is used
        ResponseEntity<ErrorResponse> response = refreshTokenExpectingError(malformedToken);

        // Then should be unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .contains("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("Given expired refresh token, when try to refresh, then should be rejected")
    void givenExpiredRefreshToken_whenTryToRefresh_thenShouldBeRejected() {
        // Given an expired refresh token (simulate by creating token with past expiration)
        // For this test, we'll use a token that was valid but is now expired
        LoginResponse loginResponse = loginAndGetTokens("dev@prjctmng.com", "prjctmng432!dev");
        String refreshToken = loginResponse.refreshToken();
        String jti = extractJtiFromRefreshToken(refreshToken);

        // Manually remove the token from Redis to simulate expiration cleanup
        redisTemplate.delete("refresh_token:" + jti);

        // When expired refresh token is used
        ResponseEntity<ErrorResponse> response = refreshTokenExpectingError(refreshToken);

        // Then should be rejected
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .contains("Invalid or expired refresh token");

        // And token should not be in Redis storage (simulating cleanup)
        assertThat(isRefreshTokenInRedis(jti))
                .as("Expired token should not be in Redis storage")
                .isFalse();
    }
}