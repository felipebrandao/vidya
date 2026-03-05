package br.com.felipebrandao.vidya.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255)
        String nome,

        @NotBlank(message = "CNPJ/CPF é obrigatório")
        @Size(max = 20)
        String cgcCpf,

        @NotBlank(message = "Nome do parceiro é obrigatório")
        @Size(max = 255)
        String nomeparc,

        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 255)
        String razaoSocial,

        @NotNull(message = "Código da cidade é obrigatório")
        Integer codcid,

        @NotBlank(message = "Tipo de pessoa é obrigatório")
        @Pattern(regexp = "[FJ]", message = "Tipo de pessoa deve ser 'F' (Física) ou 'J' (Jurídica)")
        String tipPessoa,

        @NotBlank(message = "Classificação de ICMS é obrigatória")
        @Size(max = 10)
        String classificMs
) {}

