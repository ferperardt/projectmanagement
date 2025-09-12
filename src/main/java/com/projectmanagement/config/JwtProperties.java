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
    
    @Override
    public String toString() {
        return "JwtProperties{" +
                "secret='[PROTECTED]'" +
                ", expiration=" + expiration +
                ", issuer='" + issuer + '\'' +
                '}';
    }
}