package com.projectmanagement.auth;

import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserService userService;
    
    public User registerUser(RegisterUserRequest request) {
        return userService.createUser(request);
    }
}
