package com.projectmanagement.user;

import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User createUser(RegisterUserRequest request) {
        log.debug("Creating user with username: {}", request.username());
        
        validateUserDoesNotExist(request.username(), request.email());
        
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return savedUser;
    }
    
    private void validateUserDoesNotExist(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("username", username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email", email);
        }
    }
}
