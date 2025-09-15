package com.projectmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.auth.dto.LoginRequest;
import com.projectmanagement.auth.dto.LoginResponse;
import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.exception.dto.ErrorResponse;
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
class LogoutIntegrationTest {

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

    private ResponseEntity<Void> logoutWithToken(String token) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    private ResponseEntity<Void> logoutWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    private ResponseEntity<ErrorResponse> tryToAccessProtectedEndpointWithToken(String token) {
        HttpHeaders headers = createAuthHeaders(token);
        RegisterUserRequest request = new RegisterUserRequest(
                "testuser",
                "test@prjctmng.com",
                "password123",
                UserRole.DEVELOPER,
                true,
                false,
                null
        );
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                baseUrl + "/register",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
    }

    @Test
    @DisplayName("Given I am logged in as developer, when I logout, then my token should be invalid for subsequent requests")
    void givenLoggedInAsDeveloper_whenLogout_thenTokenShouldBeInvalidForSubsequentRequests() {
        // Given I am logged in as developer
        String token = loginAndGetToken("dev@prjctmng.com", "prjctmng432!dev");

        // When I logout
        ResponseEntity<Void> logoutResponse = logoutWithToken(token);

        // Then logout should be successful
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // And my token should be invalid for subsequent requests to protected endpoints
        ResponseEntity<ErrorResponse> protectedResponse = tryToAccessProtectedEndpointWithToken(token);
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(protectedResponse.getBody()).isNotNull();
        assertThat(protectedResponse.getBody().message()).contains("Authentication required");
    }

    @Test
    @DisplayName("Given no authentication token, when I POST to logout endpoint, then I should get 401 Unauthorized")
    void givenNoAuthenticationToken_whenPostToLogoutEndpoint_thenShouldGet401Unauthorized() {
        // When I POST to "/api/auth/logout" without token
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                ErrorResponse.class
        );

        // Then I should get 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // And the response should indicate "Authentication required"
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Authentication required");
    }

    @Test
    @DisplayName("Given I am logged in and logout successfully, when I try to logout again, then I should get 401 Unauthorized")
    void givenLoggedInAndLogoutSuccessfully_whenTryToLogoutAgain_thenShouldGet401Unauthorized() {
        // Given I am logged in and logout successfully
        String token = loginAndGetToken("dev@prjctmng.com", "prjctmng432!dev");
        ResponseEntity<Void> firstLogout = logoutWithToken(token);
        assertThat(firstLogout.getStatusCode()).isEqualTo(HttpStatus.OK);

        // When I try to logout again with same token
        ResponseEntity<ErrorResponse> secondLogout = restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(token)),
                ErrorResponse.class
        );

        // Then I should get 401 Unauthorized
        assertThat(secondLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // And the response should indicate "Invalid or expired token"
        assertThat(secondLogout.getBody()).isNotNull();
        assertThat(secondLogout.getBody().message()).contains("Authentication required");
    }

    @ParameterizedTest
    @DisplayName("Given I am logged in with any role, when I logout, then logout should be successful")
    @CsvSource({
            "admin@prjctmng.com, prjctmng432!admin",
            "manager@prjctmng.com, prjctmng432!manager",
            "dev@prjctmng.com, prjctmng432!dev"
    })
    void givenLoggedInWithAnyRole_whenLogout_thenLogoutShouldBeSuccessful(
            String email,
            String password) {

        // Given I am logged in as user with any role
        String token = loginAndGetToken(email, password);

        // When I logout
        ResponseEntity<Void> response = logoutWithToken(token);

        // Then logout should be successful regardless of role
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // And subsequent logout attempts should be unauthorized (token blacklisted)
        ResponseEntity<ErrorResponse> secondLogout = restTemplate.exchange(
                baseUrl + "/logout",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(token)),
                ErrorResponse.class
        );
        assertThat(secondLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(secondLogout.getBody()).isNotNull();
        assertThat(secondLogout.getBody().message()).contains("Authentication required");
    }

    @Test
    @DisplayName("Given I logout successfully, when I login again with same credentials, then new login should work normally")
    void givenLogoutSuccessfully_whenLoginAgainWithSameCredentials_thenNewLoginShouldWorkNormally() {
        // Given I logout successfully
        String oldToken = loginAndGetToken("dev@prjctmng.com", "prjctmng432!dev");
        ResponseEntity<Void> logoutResponse = logoutWithToken(oldToken);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // When I login again with same credentials
        String newToken = loginAndGetToken("dev@prjctmng.com", "prjctmng432!dev");

        // Then new login should work normally
        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(oldToken);

        // And new token should work for logout
        ResponseEntity<Void> newLogoutResponse = logoutWithToken(newToken);
        assertThat(newLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}