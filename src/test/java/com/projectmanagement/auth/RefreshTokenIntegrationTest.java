package com.projectmanagement.auth;

import com.projectmanagement.auth.dto.LoginResponse;
import com.projectmanagement.auth.dto.RefreshTokenResponse;
import com.projectmanagement.config.JwtProperties;
import com.projectmanagement.exception.dto.ErrorResponse;
import com.projectmanagement.user.UserRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static com.projectmanagement.auth.TestDataConstants.ApiEndpoints;
import static com.projectmanagement.auth.TestDataConstants.TestData;
import static com.projectmanagement.auth.TestDataConstants.TestUsers;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class RefreshTokenIntegrationTest {

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

    @BeforeEach
    void setUp() {
        AuthTestFixture.cleanDatabaseAndCreateUsers(userRepository, passwordEncoder);
        JwtTestUtils.cleanRedis(redisTemplate);
    }


    @Test
    @DisplayName("Given user has logged in, when refresh token is used, then new token pair should be generated and Redis should be updated")
    void givenUserLoggedIn_whenRefreshToken_thenNewTokenPairGeneratedAndRedisUpdated() {
        // Given user has logged in and received tokens
        LoginResponse loginResponse = AuthTestUtils.loginAndGetTokens(restTemplate, TestUsers.DEVELOPER_EMAIL, TestUsers.DEVELOPER_PASSWORD);
        String originalAccessToken = loginResponse.accessToken();
        String originalRefreshToken = loginResponse.refreshToken();

        // Verify original refresh token is stored in Redis
        String originalJti = JwtTestUtils.extractJtiFromRefreshToken(originalRefreshToken, jwtProperties);
        assertThat(JwtTestUtils.isRefreshTokenInRedis(redisTemplate, originalJti))
                .as("Original refresh token should be stored in Redis")
                .isTrue();

        // When refresh token is used
        ResponseEntity<RefreshTokenResponse> refreshResponse = AuthTestUtils.refreshToken(restTemplate, originalRefreshToken);

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
        JwtTestUtils.verifyTokenRotation(redisTemplate, jwtProperties, originalRefreshToken, tokenResponse.refreshToken());
    }

    @Test
    @DisplayName("Given refresh token was used, when same token is used again, then should be rejected and remain blacklisted")
    void givenUsedRefreshToken_whenTryToReuseToken_thenShouldBeRejectedAndRemainBlacklisted() {
        // Given refresh token was used successfully
        LoginResponse loginResponse = AuthTestUtils.loginAndGetTokens(restTemplate, TestUsers.DEVELOPER_EMAIL, TestUsers.DEVELOPER_PASSWORD);
        String refreshToken = loginResponse.refreshToken();

        // First use should succeed
        ResponseEntity<RefreshTokenResponse> firstRefresh = AuthTestUtils.refreshToken(restTemplate, refreshToken);
        assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        String jti = JwtTestUtils.extractJtiFromRefreshToken(refreshToken, jwtProperties);
        assertThat(JwtTestUtils.isRefreshTokenBlacklisted(redisTemplate, jti))
                .as("Token should be blacklisted after first use")
                .isTrue();

        // When same token is used again
        ResponseEntity<ErrorResponse> secondRefresh = AuthTestUtils.refreshTokenExpectingError(restTemplate, refreshToken);

        // Then should be rejected
        assertThat(secondRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(secondRefresh.getBody()).isNotNull();
        assertThat(secondRefresh.getBody().message())
                .contains("Invalid or expired refresh token");

        // And token should remain blacklisted
        assertThat(JwtTestUtils.isRefreshTokenBlacklisted(redisTemplate, jti))
                .as("Token should remain blacklisted")
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {TestData.EMPTY_TOKEN, TestData.BLANK_TOKEN})
    @DisplayName("Given blank refresh token, when try to refresh, then should get validation error")
    void givenBlankRefreshToken_whenTryToRefresh_thenShouldGetValidationError(String blankToken) {
        // When blank refresh token is used
        ResponseEntity<ErrorResponse> response = AuthTestUtils.refreshTokenExpectingError(restTemplate, blankToken);

        // Then should get validation error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .containsAnyOf("Refresh token is required", "Request validation failed");
    }

    @ParameterizedTest
    @ValueSource(strings = {TestData.INVALID_TOKEN, TestData.MALFORMED_JWT_TOKEN, TestData.INVALID_SIGNATURE_TOKEN})
    @DisplayName("Given malformed refresh token, when try to refresh, then should be unauthorized")
    void givenMalformedRefreshToken_whenTryToRefresh_thenShouldBeUnauthorized(String malformedToken) {
        // When malformed refresh token is used
        ResponseEntity<ErrorResponse> response = AuthTestUtils.refreshTokenExpectingError(restTemplate, malformedToken);

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
        LoginResponse loginResponse = AuthTestUtils.loginAndGetTokens(restTemplate, TestUsers.DEVELOPER_EMAIL, TestUsers.DEVELOPER_PASSWORD);
        String refreshToken = loginResponse.refreshToken();
        String jti = JwtTestUtils.extractJtiFromRefreshToken(refreshToken, jwtProperties);

        // Manually remove the token from Redis to simulate expiration cleanup
        redisTemplate.delete("refresh_token:" + jti);

        // When expired refresh token is used
        ResponseEntity<ErrorResponse> response = AuthTestUtils.refreshTokenExpectingError(restTemplate, refreshToken);

        // Then should be rejected
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .contains("Invalid or expired refresh token");

        // And token should not be in Redis storage (simulating cleanup)
        assertThat(JwtTestUtils.isRefreshTokenInRedis(redisTemplate, jti))
                .as("Expired token should not be in Redis storage")
                .isFalse();
    }
}