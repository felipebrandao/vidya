package br.com.felipebrandao.vidya.dto.sankhya;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SankhyaField(
        @JsonProperty("$") String value
) {}

