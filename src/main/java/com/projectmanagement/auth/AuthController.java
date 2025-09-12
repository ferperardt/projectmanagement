package com.projectmanagement.auth;

import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterUserRequest registerUserRequest) {
        User createdUser = authService.registerUser(registerUserRequest);
        URI location = URI.create("/api/users/" + createdUser.getId());
        return ResponseEntity.created(location).build();
    }
}
