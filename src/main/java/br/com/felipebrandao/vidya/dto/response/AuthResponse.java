package br.com.felipebrandao.vidya.dto.response;

import java.time.Instant;

public record AuthResponse(
        String token,
        String username,
        Instant expiresAt
) {}

