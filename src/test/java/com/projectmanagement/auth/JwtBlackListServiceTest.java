package com.projectmanagement.auth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT BlackList Service")
class JwtBlackListServiceTest {

    @Mock
    private JwtBlackListProperties properties;

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

    @Nested
    @DisplayName("Token Blacklisting Operations")
    class TokenBlacklistingOperations {

        @Test
        @DisplayName("Should successfully blacklist token with valid JTI")
        void given_validJtiAndExpiration_when_blacklistToken_then_tokenIsBlacklisted() {
            // Given & When
            jwtBlackListService.blacklistToken(VALID_JTI, getFutureExpiration());

            // Then
            assertThat(jwtBlackListService.isBlacklisted(VALID_JTI)).isTrue();
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(1);
        }

        @ParameterizedTest
        @DisplayName("Should handle null or empty JTI gracefully")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void given_nullOrEmptyJti_when_blacklistToken_then_shouldNotBlacklist(String invalidJti) {
            // Given & When
            jwtBlackListService.blacklistToken(invalidJti, getFutureExpiration());

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isZero();
        }

        @Test
        @DisplayName("Should trigger lazy cleanup when enabled after blacklisting")
        void given_lazyCleanupEnabled_when_blacklistToken_then_shouldTriggerCleanup() {
            // Given
            when(properties.isEnableLazyCleanup()).thenReturn(true);
            jwtBlackListService.blacklistToken("expired-token", getPastExpiration());

            // When
            jwtBlackListService.blacklistToken(VALID_JTI, getFutureExpiration());

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(1);
            assertThat(jwtBlackListService.isBlacklisted(VALID_JTI)).isTrue();
            assertThat(jwtBlackListService.isBlacklisted("expired-token")).isFalse();
        }

        @Test
        @DisplayName("Should not trigger lazy cleanup when disabled")
        void given_lazyCleanupDisabled_when_blacklistToken_then_shouldNotTriggerCleanup() {
            // Given
            when(properties.isEnableLazyCleanup()).thenReturn(false);
            jwtBlackListService.blacklistToken("expired-token", getPastExpiration());

            // When
            jwtBlackListService.blacklistToken(VALID_JTI, getFutureExpiration());

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Blacklist Verification")
    class BlacklistVerification {

        @Test
        @DisplayName("Should return true for blacklisted token")
        void given_blacklistedToken_when_isBlacklisted_then_shouldReturnTrue() {
            // Given
            jwtBlackListService.blacklistToken(VALID_JTI, getFutureExpiration());

            // When & Then
            assertThat(jwtBlackListService.isBlacklisted(VALID_JTI)).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-blacklisted token")
        void given_nonBlacklistedToken_when_isBlacklisted_then_shouldReturnFalse() {
            // Given
            jwtBlackListService.blacklistToken(VALID_JTI, getFutureExpiration());

            // When & Then
            assertThat(jwtBlackListService.isBlacklisted(ANOTHER_JTI)).isFalse();
        }

        @ParameterizedTest
        @DisplayName("Should return false for null or empty JTI")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void given_nullOrEmptyJti_when_isBlacklisted_then_shouldReturnFalse(String invalidJti) {
            // When & Then
            assertThat(jwtBlackListService.isBlacklisted(invalidJti)).isFalse();
        }

        @Test
        @DisplayName("Should trigger lazy cleanup when enabled during verification")
        void given_lazyCleanupEnabled_when_isBlacklisted_then_shouldTriggerCleanup() {
            // Given
            when(properties.isEnableLazyCleanup()).thenReturn(true);
            jwtBlackListService.blacklistToken("expired-token", getPastExpiration());
            jwtBlackListService.blacklistToken(VALID_JTI, getFutureExpiration());

            // When
            boolean result = jwtBlackListService.isBlacklisted(VALID_JTI);

            // Then
            assertThat(result).isTrue();
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(1); // expired token removed
        }

        @Test
        @DisplayName("Should not trigger lazy cleanup when disabled")
        void given_lazyCleanupDisabled_when_isBlacklisted_then_shouldNotTriggerCleanup() {
            // Given
            when(properties.isEnableLazyCleanup()).thenReturn(false);
            jwtBlackListService.blacklistToken("expired-token", getPastExpiration());
            jwtBlackListService.blacklistToken(VALID_JTI, getFutureExpiration());

            // When
            boolean result = jwtBlackListService.isBlacklisted(VALID_JTI);

            // Then
            assertThat(result).isTrue();
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(2); // expired token not removed
        }
    }

    @Nested
    @DisplayName("Cleanup Operations")
    class CleanupOperations {

        @Test
        @DisplayName("Should remove only expired tokens during cleanup")
        void given_mixedTokens_when_performCleanup_then_shouldRemoveOnlyExpiredTokens() {
            // Given
            when(properties.isEnableLazyCleanup()).thenReturn(true);
            jwtBlackListService.blacklistToken("expired-1", getPastExpiration());
            jwtBlackListService.blacklistToken("expired-2", getCurrentTime() - 1000);
            jwtBlackListService.blacklistToken("valid-1", getFutureExpiration());
            jwtBlackListService.blacklistToken("valid-2", getCurrentTime() + 60000);

            // When - Trigger cleanup via isBlacklisted (which calls lazy cleanup)
            jwtBlackListService.isBlacklisted("any-token");

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(2);
            assertThat(jwtBlackListService.isBlacklisted("valid-1")).isTrue();
            assertThat(jwtBlackListService.isBlacklisted("valid-2")).isTrue();
            assertThat(jwtBlackListService.isBlacklisted("expired-1")).isFalse();
            assertThat(jwtBlackListService.isBlacklisted("expired-2")).isFalse();
        }

        @Test
        @DisplayName("Should keep all tokens when none are expired")
        void given_allValidTokens_when_performCleanup_then_shouldKeepAllTokens() {
            // Given
            when(properties.isEnableLazyCleanup()).thenReturn(true);
            jwtBlackListService.blacklistToken("valid-1", getFutureExpiration());
            jwtBlackListService.blacklistToken("valid-2", getCurrentTime() + 60000);
            jwtBlackListService.blacklistToken("valid-3", getCurrentTime() + 300000);

            // When
            jwtBlackListService.isBlacklisted("any-token");

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(3);
            assertThat(jwtBlackListService.isBlacklisted("valid-1")).isTrue();
            assertThat(jwtBlackListService.isBlacklisted("valid-2")).isTrue();
            assertThat(jwtBlackListService.isBlacklisted("valid-3")).isTrue();
        }

        @Test
        @DisplayName("Should remove all tokens when all are expired")
        void given_allExpiredTokens_when_performCleanup_then_shouldRemoveAllTokens() {
            // Given
            when(properties.isEnableLazyCleanup()).thenReturn(true);
            jwtBlackListService.blacklistToken("expired-1", getPastExpiration());
            jwtBlackListService.blacklistToken("expired-2", getCurrentTime() - 1000);
            jwtBlackListService.blacklistToken("expired-3", getCurrentTime() - 300000);

            // When
            jwtBlackListService.isBlacklisted("any-token");

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isZero();
            assertThat(jwtBlackListService.isBlacklisted("expired-1")).isFalse();
            assertThat(jwtBlackListService.isBlacklisted("expired-2")).isFalse();
            assertThat(jwtBlackListService.isBlacklisted("expired-3")).isFalse();
        }

        @Test
        @DisplayName("Should handle empty blacklist during cleanup")
        void given_emptyBlacklist_when_performCleanup_then_shouldHandleGracefully() {
            // Given - empty blacklist

            // When & Then - should not throw exception
            assertThat(jwtBlackListService.isBlacklisted("any-token")).isFalse();
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Scheduled Tasks")
    class ScheduledTasks {

        @Test
        @DisplayName("Should perform scheduled cleanup when enabled")
        void given_scheduledCleanupEnabled_when_performScheduledCleanup_then_shouldCleanupTokens() {
            // Given
            when(properties.isEnableScheduledCleanup()).thenReturn(true);
            jwtBlackListService.blacklistToken("expired-token", getPastExpiration());
            jwtBlackListService.blacklistToken("valid-token", getFutureExpiration());

            // When
            jwtBlackListService.performScheduledCleanup();

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(1);
            assertThat(jwtBlackListService.isBlacklisted("valid-token")).isTrue();
            assertThat(jwtBlackListService.isBlacklisted("expired-token")).isFalse();
        }

        @Test
        @DisplayName("Should skip scheduled cleanup when disabled")
        void given_scheduledCleanupDisabled_when_performScheduledCleanup_then_shouldSkipCleanup() {
            // Given
            when(properties.isEnableScheduledCleanup()).thenReturn(false);
            jwtBlackListService.blacklistToken("expired-token", getPastExpiration());
            jwtBlackListService.blacklistToken("valid-token", getFutureExpiration());

            // When
            jwtBlackListService.performScheduledCleanup();

            // Then - expired token should still be there
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle scheduled cleanup with empty blacklist")
        void given_emptyBlacklist_when_performScheduledCleanup_then_shouldHandleGracefully() {
            // Given
            when(properties.isEnableScheduledCleanup()).thenReturn(true);

            // When & Then - should not throw exception
            jwtBlackListService.performScheduledCleanup();
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {

        @Test
        @DisplayName("Should return correct blacklisted tokens count")
        void given_multipleTokens_when_getBlacklistedTokensCount_then_shouldReturnCorrectCount() {
            // Given
            jwtBlackListService.blacklistToken("token-1", getFutureExpiration());
            jwtBlackListService.blacklistToken("token-2", getFutureExpiration());
            jwtBlackListService.blacklistToken("token-3", getFutureExpiration());

            // When & Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero count for empty blacklist")
        void given_emptyBlacklist_when_getBlacklistedTokensCount_then_shouldReturnZero() {
            // When & Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isZero();
        }

        @Test
        @DisplayName("Should clear all blacklisted tokens")
        void given_multipleTokens_when_clearAll_then_shouldRemoveAllTokens() {
            // Given
            jwtBlackListService.blacklistToken("token-1", getFutureExpiration());
            jwtBlackListService.blacklistToken("token-2", getFutureExpiration());
            jwtBlackListService.blacklistToken("token-3", getFutureExpiration());
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(3);

            // When
            jwtBlackListService.clearAll();

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isZero();
            assertThat(jwtBlackListService.isBlacklisted("token-1")).isFalse();
            assertThat(jwtBlackListService.isBlacklisted("token-2")).isFalse();
            assertThat(jwtBlackListService.isBlacklisted("token-3")).isFalse();
        }

        @Test
        @DisplayName("Should handle clear all on empty blacklist")
        void given_emptyBlacklist_when_clearAll_then_shouldHandleGracefully() {
            // When & Then - should not throw exception
            jwtBlackListService.clearAll();
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isZero();
        }

        @ParameterizedTest
        @DisplayName("Should handle concurrent operations safely")
        @CsvSource({
                "token-1, token-2, token-3",
                "jti-a, jti-b, jti-c",
                "test-1, test-2, test-3"
        })
        void given_concurrentOperations_when_multipleThreadsAccess_then_shouldBeSafe(String token1, String token2, String token3) {
            // Given & When - simulate concurrent access
            jwtBlackListService.blacklistToken(token1, getFutureExpiration());
            jwtBlackListService.blacklistToken(token2, getFutureExpiration());
            jwtBlackListService.blacklistToken(token3, getFutureExpiration());

            // Then
            assertThat(jwtBlackListService.getBlacklistedTokensCount()).isEqualTo(3);
            assertThat(jwtBlackListService.isBlacklisted(token1)).isTrue();
            assertThat(jwtBlackListService.isBlacklisted(token2)).isTrue();
            assertThat(jwtBlackListService.isBlacklisted(token3)).isTrue();
        }
    }
}