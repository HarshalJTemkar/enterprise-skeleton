package com.enterprise.auth.api;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {}
