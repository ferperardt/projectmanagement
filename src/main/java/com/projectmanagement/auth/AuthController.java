package com.projectmanagement.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<Void> register() {
        String userId = UUID.randomUUID().toString();
        URI location = URI.create("/api/users/" + userId);
        return ResponseEntity.created(location).build();
    }
}
