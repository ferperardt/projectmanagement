package com.projectmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.auth.dto.LoginRequest;
import com.projectmanagement.auth.dto.LoginResponse;
import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserRepository;
import com.projectmanagement.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/auth";

        // Clean database
        userRepository.deleteAll();

        // Create test users
        createAndSaveUser("admin@prjctmng.com", "admin", "prjctmng432!admin", UserRole.ADMIN);
        createAndSaveUser("manager@prjctmng.com", "manager", "prjctmng432!manager", UserRole.PROJECT_MANAGER);
        createAndSaveUser("dev@prjctmng.com", "dev", "prjctmng432!dev", UserRole.DEVELOPER);
    }

    private void createAndSaveUser(String email, String username, String password, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setTwoFactorEnabled(false);

        userRepository.save(user);
    }

    private String loginAndGetToken(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/login",
                loginRequest,
                LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody().accessToken();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<Void> registerUser(RegisterUserRequest request, String authToken) {
        HttpHeaders headers = createAuthHeaders(authToken);
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                baseUrl + "/register",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    private RegisterUserRequest createDeveloperRequest(String username, String email) {
        return new RegisterUserRequest(
                username,
                email,
                "password123",
                UserRole.DEVELOPER,
                true,
                false,
                null
        );
    }

    @ParameterizedTest
    @DisplayName("Given an authenticated ADMIN user, when registering a new DEVELOPER, then the registration should succeed")
    @CsvSource({
            "admin@prjctmng.com, prjctmng432!admin, ADMIN, newdev1, newdev1@prjctmng.com, 201"
    })
    void givenAuthenticatedAdminUser_whenRegisteringNewDeveloper_thenRegistrationShouldSucceed(
            String userEmail,
            String userPassword,
            String userRole,
            String newUsername,
            String newEmail,
            int expectedStatus) {

        // Given an authenticated ADMIN user
        String authToken = loginAndGetToken(userEmail, userPassword);
        RegisterUserRequest newDeveloper = createDeveloperRequest(newUsername, newEmail);

        // When registering a new DEVELOPER
        ResponseEntity<Void> response = registerUser(newDeveloper, authToken);

        // Then the registration should succeed
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        // And the user should exist in the database with correct attributes
        User createdUser = userRepository.findByEmail(newEmail).orElse(null);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo(newUsername);
        assertThat(createdUser.getRole()).isEqualTo(UserRole.DEVELOPER);
        assertThat(createdUser.getEnabled()).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Given non-ADMIN authenticated user, when registering a new DEVELOPER, then the registration should be FORBIDDEN")
    @CsvSource({
            "manager@prjctmng.com, prjctmng432!manager, PROJECT_MANAGER, newdev2, newdev2@prjctmng.com, 403",
            "dev@prjctmng.com, prjctmng432!dev, DEVELOPER, newdev3, newdev3@prjctmng.com, 403"
    })
    void givenNonAdminAuthenticatedUser_whenRegisteringNewDeveloper_thenRegistrationShouldBeForbidden(
            String userEmail,
            String userPassword,
            String userRole,
            String newUsername,
            String newEmail,
            int expectedStatus) {

        // Given a non-ADMIN authenticated user with role: {userRole}
        String authToken = loginAndGetToken(userEmail, userPassword);
        RegisterUserRequest newDeveloper = createDeveloperRequest(newUsername, newEmail);

        // When registering a new DEVELOPER
        ResponseEntity<Void> response = registerUser(newDeveloper, authToken);

        // Then the registration should be FORBIDDEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // And the user should NOT exist in the database
        User createdUser = userRepository.findByEmail(newEmail).orElse(null);
        assertThat(createdUser).isNull();
    }

    @Test
    @DisplayName("Given unauthenticated user, when registering a new DEVELOPER, then the registration should be UNAUTHORIZED")
    void givenUnauthenticatedUser_whenRegisteringNewDeveloper_thenRegistrationShouldBeUnauthorized() {
        // Given an unauthenticated user (no auth token)
        String newEmail = "newdev4@prjctmng.com";
        RegisterUserRequest newDeveloper = createDeveloperRequest("newdev4", newEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // No Authorization header set - unauthenticated request
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(newDeveloper, headers);

        // When registering a new DEVELOPER without authentication
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/register",
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Then the registration should be UNAUTHORIZED
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // And the user should NOT exist in the database
        User createdUser = userRepository.findByEmail(newEmail).orElse(null);
        assertThat(createdUser).isNull();
    }
}