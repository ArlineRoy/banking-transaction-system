package com.project.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}