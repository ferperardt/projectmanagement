package com.projectmanagement.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtBlackListService {

    private final JwtBlackListProperties properties;
    private final ConcurrentHashMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String jti, long expirationTime) {
        if (jti == null || jti.trim().isEmpty()) {
            log.warn("Attempted to blacklist token with null or empty JTI");
            return;
        }

        blacklistedTokens.put(jti, expirationTime);
        log.debug("Token blacklisted: JTI={}, expiration={}", jti, expirationTime);

        if (properties.isEnableLazyCleanup()) {
            performLazyCleanup();
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return false;
        }

        if (properties.isEnableLazyCleanup()) {
            performLazyCleanup();
        }

        boolean isBlacklisted = blacklistedTokens.containsKey(jti);
        log.debug("Blacklist check: JTI={}, isBlacklisted={}", jti, isBlacklisted);

        return isBlacklisted;
    }

    @Scheduled(fixedRateString = "#{@jwtBlackListProperties.cleanupIntervalMillis}")
    public void performScheduledCleanup() {
        if (!properties.isEnableScheduledCleanup()) {
            return;
        }

        int removedCount = performCleanup();
        log.info("Scheduled blacklist cleanup completed. Removed {} expired tokens. Active tokens: {}",
                removedCount, blacklistedTokens.size());
    }

    private void performLazyCleanup() {
        int removedCount = performCleanup();
        if (removedCount > 0) {
            log.debug("Lazy cleanup removed {} expired tokens. Active tokens: {}",
                    removedCount, blacklistedTokens.size());
        }
    }

    private int performCleanup() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        Iterator<Map.Entry<String, Long>> iterator = blacklistedTokens.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < currentTime) {
                iterator.remove();
                removedCount++;
            }
        }

        return removedCount;
    }

    public int getBlacklistedTokensCount() {
        return blacklistedTokens.size();
    }

    public void clearAll() {
        int clearedCount = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.info("Cleared all blacklisted tokens. Removed {} tokens", clearedCount);
    }
}