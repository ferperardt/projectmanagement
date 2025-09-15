package com.projectmanagement.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt.blacklist.redis")
@Getter
@Setter
public class JwtBlackListProperties {

    private String keyPrefix = "jwt:blacklist";
    private int ttlBufferSeconds = 300; // 5 minutes buffer before JWT expiration

    @Override
    public String toString() {
        return "JwtBlackListProperties{" +
                "keyPrefix='" + keyPrefix + '\'' +
                ", ttlBufferSeconds=" + ttlBufferSeconds +
                '}';
    }
}