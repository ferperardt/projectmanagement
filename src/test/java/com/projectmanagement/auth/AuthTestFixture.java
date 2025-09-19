package com.projectmanagement.auth;

import com.projectmanagement.user.User;
import com.projectmanagement.user.UserRepository;
import com.projectmanagement.user.enums.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.projectmanagement.auth.TestDataConstants.TestUsers;

public final class AuthTestFixture {

    private AuthTestFixture() {
        // Utility class
    }

    public static void cleanDatabaseAndCreateUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        cleanDatabase(userRepository);
        createStandardTestUsers(userRepository, passwordEncoder);
    }

    public static void cleanDatabase(UserRepository userRepository) {
        userRepository.deleteAll();
    }

    public static void createStandardTestUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        createTestUser(userRepository, passwordEncoder,
                      TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_USERNAME,
                      TestUsers.ADMIN_PASSWORD, TestUsers.ADMIN_ROLE);

        createTestUser(userRepository, passwordEncoder,
                      TestUsers.MANAGER_EMAIL, TestUsers.MANAGER_USERNAME,
                      TestUsers.MANAGER_PASSWORD, TestUsers.MANAGER_ROLE);

        createTestUser(userRepository, passwordEncoder,
                      TestUsers.DEVELOPER_EMAIL, TestUsers.DEVELOPER_USERNAME,
                      TestUsers.DEVELOPER_PASSWORD, TestUsers.DEVELOPER_ROLE);
    }

    public static void createTestUser(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    String email,
                                    String username,
                                    String password,
                                    UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setTwoFactorEnabled(false);

        userRepository.save(user);
    }
}