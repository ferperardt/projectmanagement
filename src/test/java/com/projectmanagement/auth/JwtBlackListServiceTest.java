package com.projectmanagement.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWT BlackList Service")
class JwtBlackListServiceTest {

    @Mock
    private JwtBlackListProperties properties;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtBlackListService jwtBlackListService;

    private static final String VALID_JTI = "test-jti-123";
    private static final String ANOTHER_JTI = "another-jti-456";

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private long getFutureExpiration() {
        return getCurrentTime() + 3600000; // 1 hour from now
    }

    private long getPastExpiration() {
        return getCurrentTime() - 3600000; // 1 hour ago
    }

    @BeforeEach
    void setUp() {
        // Setup default property values
        when(properties.getKeyPrefix()).thenReturn("jwt:blacklist");
        when(properties.getTtlBufferSeconds()).thenReturn(300);

        // Setup Redis template mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Token Blacklisting Operations")
    class TokenBlacklistingOperations {

        @Test
        @DisplayName("Should successfully blacklist token with valid JTI")
        void given_validJtiAndExpiration_when_blacklistToken_then_tokenIsBlacklisted() {
            // Given
            long futureExpiration = getFutureExpiration();
            String expectedKey = "jwt:blacklist:" + VALID_JTI;

            // When
            jwtBlackListService.blacklistToken(VALID_JTI, futureExpiration);

            // Then
            verify(valueOperations).set(eq(expectedKey), eq("blacklisted"), any(Duration.class));
        }

        @ParameterizedTest
        @DisplayName("Should handle null or empty JTI gracefully")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void given_nullOrEmptyJti_when_blacklistToken_then_shouldNotBlacklist(String invalidJti) {
            // Given & When
            jwtBlackListService.blacklistToken(invalidJti, getFutureExpiration());

            // Then
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("Should not blacklist already expired token")
        void given_expiredToken_when_blacklistToken_then_shouldNotStore() {
            // Given
            long pastExpiration = getPastExpiration();

            // When
            jwtBlackListService.blacklistToken(VALID_JTI, pastExpiration);

            // Then
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("Should calculate correct TTL with buffer")
        void given_validToken_when_blacklistToken_then_shouldSetCorrectTTL() {
            // Given
            long futureExpiration = getCurrentTime() + 1000000; // ~16 minutes
            String expectedKey = "jwt:blacklist:" + VALID_JTI;

            // When
            jwtBlackListService.blacklistToken(VALID_JTI, futureExpiration);

            // Then
            verify(valueOperations).set(eq(expectedKey), eq("blacklisted"), argThat(duration ->
                duration.toSeconds() >= 1000 && duration.toSeconds() <= 2000 // ~16 min + 5 min buffer
            ));
        }
    }

    @Nested
    @DisplayName("Blacklist Verification")
    class BlacklistVerification {

        @Test
        @DisplayName("Should return true for blacklisted token")
        void given_blacklistedToken_when_isBlacklisted_then_shouldReturnTrue() {
            // Given
            String expectedKey = "jwt:blacklist:" + VALID_JTI;
            when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

            // When
            boolean result = jwtBlackListService.isBlacklisted(VALID_JTI);

            // Then
            assertThat(result).isTrue();
            verify(redisTemplate).hasKey(expectedKey);
        }

        @Test
        @DisplayName("Should return false for non-blacklisted token")
        void given_nonBlacklistedToken_when_isBlacklisted_then_shouldReturnFalse() {
            // Given
            String expectedKey = "jwt:blacklist:" + ANOTHER_JTI;
            when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

            // When
            boolean result = jwtBlackListService.isBlacklisted(ANOTHER_JTI);

            // Then
            assertThat(result).isFalse();
            verify(redisTemplate).hasKey(expectedKey);
        }

        @ParameterizedTest
        @DisplayName("Should return false for null or empty JTI")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void given_nullOrEmptyJti_when_isBlacklisted_then_shouldReturnFalse(String invalidJti) {
            // When
            boolean result = jwtBlackListService.isBlacklisted(invalidJti);

            // Then
            assertThat(result).isFalse();
            verify(redisTemplate, never()).hasKey(anyString());
        }
    }


    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {

        @Test
        @DisplayName("Should return correct blacklisted tokens count")
        void given_multipleTokens_when_getBlacklistedTokensCount_then_shouldReturnCorrectCount() {
            // Given
            Set<String> mockKeys = Set.of("jwt:blacklist:token-1", "jwt:blacklist:token-2", "jwt:blacklist:token-3");
            when(redisTemplate.keys("jwt:blacklist:*")).thenReturn(mockKeys);

            // When
            int count = jwtBlackListService.getBlacklistedTokensCount();

            // Then
            assertThat(count).isEqualTo(3);
            verify(redisTemplate).keys("jwt:blacklist:*");
        }

        @Test
        @DisplayName("Should return zero count for empty blacklist")
        void given_emptyBlacklist_when_getBlacklistedTokensCount_then_shouldReturnZero() {
            // Given
            when(redisTemplate.keys("jwt:blacklist:*")).thenReturn(Set.of());

            // When
            int count = jwtBlackListService.getBlacklistedTokensCount();

            // Then
            assertThat(count).isZero();
            verify(redisTemplate).keys("jwt:blacklist:*");
        }

        @Test
        @DisplayName("Should clear all blacklisted tokens")
        void given_multipleTokens_when_clearAll_then_shouldRemoveAllTokens() {
            // Given
            Set<String> mockKeys = Set.of("jwt:blacklist:token-1", "jwt:blacklist:token-2", "jwt:blacklist:token-3");
            when(redisTemplate.keys("jwt:blacklist:*")).thenReturn(mockKeys);

            // When
            jwtBlackListService.clearAll();

            // Then
            verify(redisTemplate).keys("jwt:blacklist:*");
            verify(redisTemplate).delete(mockKeys);
        }

        @Test
        @DisplayName("Should handle clear all on empty blacklist")
        void given_emptyBlacklist_when_clearAll_then_shouldHandleGracefully() {
            // Given
            when(redisTemplate.keys("jwt:blacklist:*")).thenReturn(Set.of());

            // When
            jwtBlackListService.clearAll();

            // Then
            verify(redisTemplate).keys("jwt:blacklist:*");
            verify(redisTemplate, never()).delete(any(Collection.class));
        }

        @Test
        @DisplayName("Should handle null keys gracefully in count operation")
        void given_nullKeysFromRedis_when_getBlacklistedTokensCount_then_shouldReturnZero() {
            // Given
            when(redisTemplate.keys("jwt:blacklist:*")).thenReturn(null);

            // When
            int count = jwtBlackListService.getBlacklistedTokensCount();

            // Then
            assertThat(count).isZero();
        }
    }
}