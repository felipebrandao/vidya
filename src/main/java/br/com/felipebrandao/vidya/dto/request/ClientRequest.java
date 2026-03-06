package br.com.felipebrandao.vidya.dto.request;

import br.com.felipebrandao.vidya.entity.PersonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientRequest(

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

        @NotNull(message = "Tipo de pessoa é obrigatório")
        PersonType tipPessoa,

        @NotBlank(message = "Classificação de ICMS é obrigatória")
        @Size(max = 10)
        String classificMs
) {}

