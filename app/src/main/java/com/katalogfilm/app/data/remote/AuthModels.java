package com.katalogfilm.app.data.remote;

import com.google.gson.annotations.SerializedName;

public final class AuthModels {
    private AuthModels() { }

    public static class RegisterRequest {
        public String name;
        public String email;
        public String password;
        public RegisterRequest(String name, String email, String password) {
            this.name = name; this.email = email; this.password = password;
        }
    }

    public static class LoginRequest {
        public String email;
        public String password;
        public LoginRequest(String email, String password) {
            this.email = email; this.password = password;
        }
    }

    public static class GoogleRequest {
        @SerializedName("idToken") public String idToken;
        public GoogleRequest(String idToken) { this.idToken = idToken; }
    }

    public static class ForgotPasswordRequest {
        public String email;
        public ForgotPasswordRequest(String email) { this.email = email; }
    }

    public static class AuthResponse {
        public String message;
        public String token;
        public User user;
    }

    public static class MessageResponse {
        public String message;
    }

    public static class User {
        public long id;
        public String name;
        public String email;
    }
}
