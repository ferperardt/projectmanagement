package com.projectmanagement.auth;

import com.projectmanagement.auth.dto.LoginRequest;
import com.projectmanagement.auth.dto.LoginResponse;
import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.config.JwtProperties;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;
    
    public User registerUser(RegisterUserRequest request) {
        return userService.createUser(request);
    }
    
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        User user = userService.findByEmail(request.getEmail());
        String jwt = jwtService.generateToken(user);
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name()
        );
        
        return new LoginResponse(jwt, jwtProperties.getExpiration() / 1000, userInfo);
    }
}
