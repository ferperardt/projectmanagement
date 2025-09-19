package com.projectmanagement.user;

import com.projectmanagement.user.dto.UserResponse;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static com.projectmanagement.user.UserTestConstants.*;

public final class UserTestUtils {

    private UserTestUtils() {
        // Utility class
    }

    public static ResponseEntity<UserResponse> getCurrentUser(TestRestTemplate restTemplate, String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                ApiEndpoints.ME_URL,
                HttpMethod.GET,
                entity,
                UserResponse.class
        );
    }
}