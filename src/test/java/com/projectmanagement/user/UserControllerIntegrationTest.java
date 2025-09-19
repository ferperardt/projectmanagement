package com.projectmanagement.user;

import com.projectmanagement.auth.AuthTestFixture;
import com.projectmanagement.auth.AuthTestUtils;
import com.projectmanagement.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class UserControllerIntegrationTest {

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

    @ParameterizedTest
    @CsvSource({
            "ADMIN, " + TestUsers.ADMIN_EMAIL + ", " + TestUsers.ADMIN_USERNAME + ", " + TestUsers.ADMIN_PASSWORD,
            "PROJECT_MANAGER, " + TestUsers.MANAGER_EMAIL + ", " + TestUsers.MANAGER_USERNAME + ", " + TestUsers.MANAGER_PASSWORD,
            "DEVELOPER, " + TestUsers.DEVELOPER_EMAIL + ", " + TestUsers.DEVELOPER_USERNAME + ", " + TestUsers.DEVELOPER_PASSWORD
    })
    @DisplayName("Given an authenticated user of any role, when accessing the /me endpoint, then should return user profile information")
    void givenAuthenticatedUser_whenAccessingMeEndpoint_thenShouldReturnUserProfileInformation(
            String expectedRole, String email, String expectedUsername, String password) {

        // Given an authenticated user
        String authToken = AuthTestUtils.loginAndGetToken(restTemplate, email, password);

        // When accessing the /me endpoint
        ResponseEntity<UserResponse> response = UserTestUtils.getCurrentUser(restTemplate, authToken);

        // Then should return user profile information
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserResponse userResponse = response.getBody();
        assertThat(userResponse.id()).isNotNull();
        assertThat(userResponse.email()).isEqualTo(email);
        assertThat(userResponse.username()).isEqualTo(expectedUsername);
        assertThat(userResponse.role()).isEqualTo(expectedRole);
        assertThat(userResponse.enabled()).isTrue();
        assertThat(userResponse.twoFactorEnabled()).isFalse();
        assertThat(userResponse.createdAt()).isNotNull();
        assertThat(userResponse.updatedAt()).isNotNull();
    }
}