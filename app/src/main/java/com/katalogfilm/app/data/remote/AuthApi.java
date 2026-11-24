package com.katalogfilm.app.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/auth/register")
    Call<AuthModels.AuthResponse> register(@Body AuthModels.RegisterRequest body);

    @POST("api/auth/login")
    Call<AuthModels.AuthResponse> login(@Body AuthModels.LoginRequest body);

    @POST("api/auth/google")
    Call<AuthModels.AuthResponse> google(@Body AuthModels.GoogleRequest body);

    @POST("api/auth/forgot-password")
    Call<AuthModels.MessageResponse> forgotPassword(@Body AuthModels.ForgotPasswordRequest body);
}
