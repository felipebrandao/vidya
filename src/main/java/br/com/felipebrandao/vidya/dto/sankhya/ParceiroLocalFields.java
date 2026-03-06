package br.com.felipebrandao.vidya.dto.sankhya;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ParceiroLocalFields(

        @JsonProperty("CGC_CPF")
        SankhyaField cgcCpf,

        @JsonProperty("NOMEPARC")
        SankhyaField nomeparc,

        @JsonProperty("RAZAOSOCIAL")
        SankhyaField razaoSocial,

        @JsonProperty("TIPPESSOA")
        SankhyaField tipPessoa,

        @JsonProperty("CLASSIFICMS")
        SankhyaField classificMs,

        @JsonProperty("CODCID")
        SankhyaField codcid
) {}

