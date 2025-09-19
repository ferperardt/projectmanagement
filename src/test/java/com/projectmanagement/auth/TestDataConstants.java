package com.projectmanagement.auth;

import com.projectmanagement.user.enums.UserRole;

public final class TestDataConstants {

    private TestDataConstants() {
        // Utility class
    }

    public static final class TestUsers {
        public static final String ADMIN_EMAIL = "admin@prjctmng.com";
        public static final String ADMIN_USERNAME = "admin";
        public static final String ADMIN_PASSWORD = "prjctmng432!admin";
        public static final UserRole ADMIN_ROLE = UserRole.ADMIN;

        public static final String MANAGER_EMAIL = "manager@prjctmng.com";
        public static final String MANAGER_USERNAME = "manager";
        public static final String MANAGER_PASSWORD = "prjctmng432!manager";
        public static final UserRole MANAGER_ROLE = UserRole.PROJECT_MANAGER;

        public static final String DEVELOPER_EMAIL = "dev@prjctmng.com";
        public static final String DEVELOPER_USERNAME = "dev";
        public static final String DEVELOPER_PASSWORD = "prjctmng432!dev";
        public static final UserRole DEVELOPER_ROLE = UserRole.DEVELOPER;

        private TestUsers() {
            // Utility class
        }
    }

    public static final class TestData {
        public static final String DEFAULT_NEW_USER_PASSWORD = "password123";
        public static final boolean DEFAULT_ENABLED_STATUS = true;
        public static final boolean DEFAULT_TWO_FACTOR_DISABLED = false;

        public static final String BEARER_TOKEN_PREFIX = "eyJ";
        public static final String BEARER_TOKEN_TYPE = "Bearer";
        public static final int ACCESS_TOKEN_EXPIRATION_SECONDS = 1800;

        public static final String EMPTY_TOKEN = "";
        public static final String BLANK_TOKEN = "   ";
        public static final String INVALID_TOKEN = "invalid-token";
        public static final String MALFORMED_JWT_TOKEN = "malformed.jwt.token";
        public static final String INVALID_SIGNATURE_TOKEN = "eyJhbGciOiJIUzI1NiJ9.invalid.signature";

        private TestData() {
            // Utility class
        }
    }

    public static final class ApiEndpoints {
        private static final String BASE_URL = "http://localhost:8089";
        private static final String AUTH_BASE = BASE_URL + "/api/auth";

        public static final String LOGIN_URL = AUTH_BASE + "/login";
        public static final String LOGOUT_URL = AUTH_BASE + "/logout";
        public static final String REGISTER_URL = AUTH_BASE + "/register";
        public static final String REFRESH_URL = AUTH_BASE + "/refresh";

        private ApiEndpoints() {
            // Utility class
        }
    }
}