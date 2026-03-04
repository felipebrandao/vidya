package br.com.felipebrandao.vidya.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Username é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        String password
) {}

