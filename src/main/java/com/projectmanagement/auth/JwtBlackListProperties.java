package com.projectmanagement.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt.blacklist")
@Getter
@Setter
public class JwtBlackListProperties {

    private long cleanupIntervalMillis = 3600000; // 1 hour in milliseconds
    private boolean enableScheduledCleanup = true;
    private boolean enableLazyCleanup = true;

    @Override
    public String toString() {
        return "JwtBlackListProperties{" +
                "cleanupIntervalMillis=" + cleanupIntervalMillis +
                ", enableScheduledCleanup=" + enableScheduledCleanup +
                ", enableLazyCleanup=" + enableLazyCleanup +
                '}';
    }
}