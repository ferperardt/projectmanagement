package com.projectmanagement.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record LoginResponse(

    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("refresh_token")
    String refreshToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    long expiresIn,

    UserInfo user
) {

    public LoginResponse(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
    
    public record UserInfo(
        UUID id,
        String email,
        String username,
        String role
    ) {}
}