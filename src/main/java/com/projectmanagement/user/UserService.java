package com.projectmanagement.user;

import com.projectmanagement.auth.dto.RegisterUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User createUser(RegisterUserRequest request) {
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        return userRepository.save(user);
    }
}
