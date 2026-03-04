package br.com.felipebrandao.vidya.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Username é obrigatório")
        @Size(min = 3, max = 100, message = "Username deve ter entre 3 e 100 caracteres")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password
) {}

