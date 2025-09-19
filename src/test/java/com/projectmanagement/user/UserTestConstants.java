package com.projectmanagement.user;

public final class UserTestConstants {

    private UserTestConstants() {
        // Utility class
    }

    public static final class ApiEndpoints {
        private static final String BASE_URL = "http://localhost:8089";
        private static final String USER_BASE = BASE_URL + "/api/users";

        public static final String ME_URL = USER_BASE + "/me";

        private ApiEndpoints() {
            // Utility class
        }
    }
}