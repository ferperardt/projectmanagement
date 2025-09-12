package com.projectmanagement.user;

import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    
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
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(!user.getEnabled())
                .credentialsExpired(false)
                .disabled(!user.getEnabled())
                .build();
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
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
