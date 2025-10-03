package com.projectmanagement.user;

import com.projectmanagement.auth.CustomUserDetails;
import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.exception.UserAlreadyExistsException;
import com.projectmanagement.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        log.debug("Loading user details for authentication: {}", email);

        User user = findByEmail(email);
        return new CustomUserDetails(user);
    }

    public User findByEmail(String email) {
        log.debug("Querying database for user with email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public UserResponse getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        log.debug("Getting current user with email: {}", email);

        return userMapper.toResponse(findByEmail(email));
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
