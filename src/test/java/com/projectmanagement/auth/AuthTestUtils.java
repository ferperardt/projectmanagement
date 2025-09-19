package com.projectmanagement.auth;

import com.projectmanagement.auth.dto.*;
import com.projectmanagement.exception.dto.ErrorResponse;
import com.projectmanagement.user.enums.UserRole;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static com.projectmanagement.auth.TestDataConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

public final class AuthTestUtils {

    private AuthTestUtils() {
        // Utility class
    }
    
    public static String loginAndGetToken(TestRestTemplate restTemplate, String email, String password) {
        LoginResponse response = loginAndGetTokens(restTemplate, email, password);
        return response.accessToken();
    }

    public static LoginResponse loginAndGetTokens(TestRestTemplate restTemplate, String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                ApiEndpoints.LOGIN_URL,
                loginRequest,
                LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    public static HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    public static HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Convenience methods for standard test users
    public static String getAdminToken(TestRestTemplate restTemplate) {
        return loginAndGetToken(restTemplate, TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_PASSWORD);
    }

    public static String getManagerToken(TestRestTemplate restTemplate) {
        return loginAndGetToken(restTemplate, TestUsers.MANAGER_EMAIL, TestUsers.MANAGER_PASSWORD);
    }

    public static String getDeveloperToken(TestRestTemplate restTemplate) {
        return loginAndGetToken(restTemplate, TestUsers.DEVELOPER_EMAIL, TestUsers.DEVELOPER_PASSWORD);
    }

    public static ResponseEntity<Void> registerUser(TestRestTemplate restTemplate,
                                                    RegisterUserRequest request,
                                                    String authToken) {
        HttpHeaders headers = createAuthHeaders(authToken);
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                ApiEndpoints.REGISTER_URL,
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public static ResponseEntity<Void> logoutWithToken(TestRestTemplate restTemplate,
                                                       String token) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                ApiEndpoints.LOGOUT_URL,
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public static ResponseEntity<ErrorResponse> tryToAccessProtectedEndpointWithToken(TestRestTemplate restTemplate,
                                                                                      String token) {
        HttpHeaders headers = createAuthHeaders(token);
        RegisterUserRequest request = createDeveloperRequest("testuser", "test@prjctmng.com");
        HttpEntity<RegisterUserRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                ApiEndpoints.REGISTER_URL,
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
    }

    public static ResponseEntity<RefreshTokenResponse> refreshToken(TestRestTemplate restTemplate,
                                                                    String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        HttpHeaders headers = createJsonHeaders();
        HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                ApiEndpoints.REFRESH_URL,
                HttpMethod.POST,
                entity,
                RefreshTokenResponse.class
        );
    }

    public static ResponseEntity<ErrorResponse> refreshTokenExpectingError(TestRestTemplate restTemplate,
                                                                           String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        HttpHeaders headers = createJsonHeaders();
        HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(
                ApiEndpoints.REFRESH_URL,
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
    }

    public static ResponseEntity<ErrorResponse> logoutWithoutToken(TestRestTemplate restTemplate) {
        HttpHeaders headers = createJsonHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                ApiEndpoints.LOGOUT_URL,
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
    }

    public static ResponseEntity<ErrorResponse> logoutWithTokenExpectingError(TestRestTemplate restTemplate,
                                                                              String token) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                ApiEndpoints.LOGOUT_URL,
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
    }

    // Request builders
    public static RegisterUserRequest createUserRequest(String username, String email, UserRole role) {
        return new RegisterUserRequest(
                username,
                email,
                TestData.DEFAULT_NEW_USER_PASSWORD,
                role,
                TestData.DEFAULT_ENABLED_STATUS,
                TestData.DEFAULT_TWO_FACTOR_DISABLED,
                null
        );
    }

    public static RegisterUserRequest createDeveloperRequest(String username, String email) {
        return createUserRequest(username, email, UserRole.DEVELOPER);
    }
}