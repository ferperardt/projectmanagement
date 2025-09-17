package com.projectmanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long expiration = 86400000; // 24 hours in milliseconds
    private String issuer = "project-management-system";

    private String refreshSecret;
    private long refreshExpiration = 604800000; // 7 days in milliseconds
    
    @Override
    public String toString() {
        return "JwtProperties{" +
                "secret='[PROTECTED]'" +
                ", expiration=" + expiration +
                ", issuer='" + issuer + '\'' +
                ", refreshSecret='[PROTECTED]'" +
                ", refreshExpiration=" + refreshExpiration +
                '}';
    }
}