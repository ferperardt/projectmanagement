package com.projectmanagement.auth;

import com.projectmanagement.auth.dto.LoginRequest;
import com.projectmanagement.auth.dto.LoginResponse;
import com.projectmanagement.auth.dto.RefreshTokenRequest;
import com.projectmanagement.auth.dto.RefreshTokenResponse;
import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.config.JwtProperties;
import com.projectmanagement.exception.InvalidRefreshTokenException;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;
    private final JwtBlackListService jwtBlackListService;
    private final RefreshTokenService refreshTokenService;

    public User registerUser(RegisterUserRequest request) {
        return userService.createUser(request);
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userService.findByEmail(request.email());
        String jwt = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.generateRefreshToken(user);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name()
        );

        return new LoginResponse(jwt, refreshToken, jwtProperties.getExpiration() / 1000, userInfo);
    }

    public void logout(String token) {
        String jti = jwtService.extractJti(token);
        long expirationTime = jwtService.extractExpiration(token).getTime();
        jwtBlackListService.blacklistToken(jti, expirationTime);
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!refreshTokenService.isValidRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        User user = refreshTokenService.getUserFromRefreshToken(refreshToken);

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.generateRefreshToken(user);

        refreshTokenService.invalidateRefreshToken(refreshToken);

        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getExpiration() / 1000
        );
    }
}
