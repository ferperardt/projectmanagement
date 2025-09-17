package com.projectmanagement.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenResponse(

    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("refresh_token")
    String refreshToken,

    @JsonProperty("expires_in")
    long expiresIn,

    @JsonProperty("token_type")
    String tokenType

) {

    public RefreshTokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, expiresIn, "Bearer");
    }

}