package br.com.felipebrandao.vidya.dto.response;

public record AuthResponse(
        String token,
        String username,
        long expiresIn
) {}

