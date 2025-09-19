package com.projectmanagement.auth;

import com.projectmanagement.auth.dto.LoginResponse;
import com.projectmanagement.auth.dto.RegisterUserRequest;
import com.projectmanagement.user.User;
import com.projectmanagement.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static com.projectmanagement.auth.TestDataConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        AuthTestFixture.cleanDatabaseAndCreateUsers(userRepository, passwordEncoder);
    }


    @Test
    @DisplayName("Given an authenticated ADMIN user, when registering a new DEVELOPER, then the registration should succeed")
    void givenAuthenticatedAdminUser_whenRegisteringNewDeveloper_thenRegistrationShouldSucceed() {

        // Given an authenticated ADMIN user
        String authToken = AuthTestUtils.getAdminToken(restTemplate);
        RegisterUserRequest newDeveloper = AuthTestUtils.createDeveloperRequest("newdev1", "newdev1@prjctmng.com");

        // When registering a new DEVELOPER
        ResponseEntity<Void> response = AuthTestUtils.registerUser(restTemplate, newDeveloper, authToken);

        // Then the registration should succeed
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        // And the user should exist in the database
        User createdUser = userRepository.findByEmail("newdev1@prjctmng.com").orElse(null);
        assertThat(createdUser).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("Given non-ADMIN authenticated user, when registering a new DEVELOPER, then the registration should be FORBIDDEN")
    @CsvSource({
            TestUsers.MANAGER_EMAIL + ", " + TestUsers.MANAGER_PASSWORD + ", newdev2, newdev2@prjctmng.com",
            TestUsers.DEVELOPER_EMAIL + ", " + TestUsers.DEVELOPER_PASSWORD + ", newdev3, newdev3@prjctmng.com"
    })
    void givenNonAdminAuthenticatedUser_whenRegisteringNewDeveloper_thenRegistrationShouldBeForbidden(
            String userEmail,
            String userPassword,
            String newUsername,
            String newEmail) {

        // Given a non-ADMIN authenticated user
        String authToken = AuthTestUtils.loginAndGetToken(restTemplate, userEmail, userPassword);
        RegisterUserRequest newDeveloper = AuthTestUtils.createDeveloperRequest(newUsername, newEmail);

        // When registering a new DEVELOPER
        ResponseEntity<Void> response = AuthTestUtils.registerUser(restTemplate, newDeveloper, authToken);

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
        RegisterUserRequest newDeveloper = AuthTestUtils.createDeveloperRequest("newdev4", newEmail);

        HttpHeaders headers = AuthTestUtils.createJsonHeaders();
        // No Authorization header set - unauthenticated request
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(newDeveloper, headers);

        // When registering a new DEVELOPER without authentication
        ResponseEntity<Void> response = restTemplate.exchange(
                ApiEndpoints.REGISTER_URL,
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

    @ParameterizedTest
    @DisplayName("Given valid user credentials, when logging in, then login should be successful")
    @CsvSource({
            TestUsers.ADMIN_EMAIL + ", " + TestUsers.ADMIN_PASSWORD + ", ADMIN",
            TestUsers.MANAGER_EMAIL + ", " + TestUsers.MANAGER_PASSWORD + ", PROJECT_MANAGER",
            TestUsers.DEVELOPER_EMAIL + ", " + TestUsers.DEVELOPER_PASSWORD + ", DEVELOPER"
    })
    void givenValidUserCredentials_whenLoggingIn_thenLoginShouldBeSuccessful(
            String email,
            String password,
            String expectedRole) {

        // When logging in with valid credentials
        LoginResponse loginResponse = AuthTestUtils.loginAndGetTokens(restTemplate, email, password);

        // Then login should be successful with complete token information
        assertThat(loginResponse).isNotNull();

        // Validate access token
        assertThat(loginResponse.accessToken()).isNotNull();
        assertThat(loginResponse.accessToken()).isNotEmpty();
        assertThat(loginResponse.accessToken()).startsWith(TestData.BEARER_TOKEN_PREFIX);

        // Validate refresh token
        assertThat(loginResponse.refreshToken()).isNotNull();
        assertThat(loginResponse.refreshToken()).isNotEmpty();
        assertThat(loginResponse.refreshToken()).startsWith(TestData.BEARER_TOKEN_PREFIX);

        // Validate token metadata
        assertThat(loginResponse.tokenType()).isEqualTo(TestData.BEARER_TOKEN_TYPE);
        assertThat(loginResponse.expiresIn()).isEqualTo(TestData.ACCESS_TOKEN_EXPIRATION_SECONDS);

        // Validate user information
        assertThat(loginResponse.user()).isNotNull();
        assertThat(loginResponse.user().email()).isEqualTo(email);
        assertThat(loginResponse.user().role()).isEqualTo(expectedRole);
        assertThat(loginResponse.user().id()).isNotNull();
        assertThat(loginResponse.user().username()).isNotNull();
        assertThat(loginResponse.user().username()).isNotEmpty();

        // Validate tokens are unique
        assertThat(loginResponse.accessToken()).isNotEqualTo(loginResponse.refreshToken());

        // Validate access token is shorter than refresh token (different expiration times)
        assertThat(loginResponse.accessToken().length()).isLessThan(loginResponse.refreshToken().length());
    }
}