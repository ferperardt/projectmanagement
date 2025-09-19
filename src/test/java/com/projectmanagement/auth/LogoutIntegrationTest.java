package com.projectmanagement.auth;

import com.projectmanagement.exception.dto.ErrorResponse;
import com.projectmanagement.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static com.projectmanagement.auth.TestDataConstants.ApiEndpoints;
import static com.projectmanagement.auth.TestDataConstants.TestUsers;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class LogoutIntegrationTest {

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
    @DisplayName("Given I am logged in as developer, when I logout, then my token should be invalid for subsequent requests")
    void givenLoggedInAsDeveloper_whenLogout_thenTokenShouldBeInvalidForSubsequentRequests() {
        // Given I am logged in as developer
        String token = AuthTestUtils.getDeveloperToken(restTemplate);

        // When I logout
        ResponseEntity<Void> logoutResponse = AuthTestUtils.logoutWithToken(restTemplate, token);

        // Then logout should be successful
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // And my token should be invalid for subsequent requests to protected endpoints
        ResponseEntity<ErrorResponse> protectedResponse = AuthTestUtils.tryToAccessProtectedEndpointWithToken(restTemplate, token);
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(protectedResponse.getBody()).isNotNull();
        assertThat(protectedResponse.getBody().message()).contains("Authentication required");
    }

    @Test
    @DisplayName("Given no authentication token, when I POST to logout endpoint, then I should get 401 Unauthorized")
    void givenNoAuthenticationToken_whenPostToLogoutEndpoint_thenShouldGet401Unauthorized() {
        // When I POST to "/api/auth/logout" without token
        ResponseEntity<ErrorResponse> response = AuthTestUtils.logoutWithoutToken(restTemplate);

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
        String token = AuthTestUtils.getDeveloperToken(restTemplate);
        ResponseEntity<Void> firstLogout = AuthTestUtils.logoutWithToken(restTemplate, token);
        assertThat(firstLogout.getStatusCode()).isEqualTo(HttpStatus.OK);

        // When I try to logout again with same token
        ResponseEntity<ErrorResponse> secondLogout = AuthTestUtils.logoutWithTokenExpectingError(restTemplate, token);

        // Then I should get 401 Unauthorized
        assertThat(secondLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // And the response should indicate "Invalid or expired token"
        assertThat(secondLogout.getBody()).isNotNull();
        assertThat(secondLogout.getBody().message()).contains("Authentication required");
    }

    @ParameterizedTest
    @DisplayName("Given I am logged in with any role, when I logout, then logout should be successful")
    @CsvSource({
            TestUsers.ADMIN_EMAIL + ", " + TestUsers.ADMIN_PASSWORD,
            TestUsers.MANAGER_EMAIL + ", " + TestUsers.MANAGER_PASSWORD,
            TestUsers.DEVELOPER_EMAIL + ", " + TestUsers.DEVELOPER_PASSWORD
    })
    void givenLoggedInWithAnyRole_whenLogout_thenLogoutShouldBeSuccessful(
            String email,
            String password) {

        // Given I am logged in as user with any role
        String token = AuthTestUtils.loginAndGetToken(restTemplate, email, password);

        // When I logout
        ResponseEntity<Void> response = AuthTestUtils.logoutWithToken(restTemplate, token);

        // Then logout should be successful regardless of role
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // And subsequent logout attempts should be unauthorized (token blacklisted)
        ResponseEntity<ErrorResponse> secondLogout = AuthTestUtils.logoutWithTokenExpectingError(restTemplate, token);
        assertThat(secondLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(secondLogout.getBody()).isNotNull();
        assertThat(secondLogout.getBody().message()).contains("Authentication required");
    }

    @Test
    @DisplayName("Given I logout successfully, when I login again with same credentials, then new login should work normally")
    void givenLogoutSuccessfully_whenLoginAgainWithSameCredentials_thenNewLoginShouldWorkNormally() {
        // Given I logout successfully
        String oldToken = AuthTestUtils.getDeveloperToken(restTemplate);
        ResponseEntity<Void> logoutResponse = AuthTestUtils.logoutWithToken(restTemplate, oldToken);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // When I login again with same credentials
        String newToken = AuthTestUtils.getDeveloperToken(restTemplate);

        // Then new login should work normally
        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(oldToken);

        // And new token should work for logout
        ResponseEntity<Void> newLogoutResponse = AuthTestUtils.logoutWithToken(restTemplate, newToken);
        assertThat(newLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}