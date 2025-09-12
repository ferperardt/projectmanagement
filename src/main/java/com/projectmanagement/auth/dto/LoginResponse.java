package com.projectmanagement.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    
    @JsonProperty("expires_in")
    private long expiresIn;
    
    private UserInfo user;
    
    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String email;
        private String username;
        private String role;
    }
    
    public LoginResponse(String accessToken, long expiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.tokenType = "Bearer";
    }
}