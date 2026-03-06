package br.com.felipebrandao.vidya.dto.sankhya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SankhyaSaveResponse(

        @JsonProperty("serviceName")
        String serviceName,

        @JsonProperty("status")
        String status,

        @JsonProperty("statusMessage")
        String statusMessage,

        @JsonProperty("tsError")
        TsError tsError,

        @JsonProperty("responseBody")
        ResponseBody responseBody
) {

    public boolean isSuccess() {
        return "1".equals(status);
    }

    public String errorMessage() {
        if (statusMessage != null && !statusMessage.isBlank()) {
            return statusMessage;
        }
        if (tsError != null && tsError.tsErrorCode() != null) {
            return "Erro Sankhya [" + tsError.tsErrorCode() + "]";
        }
        return "Erro desconhecido retornado pelo Sankhya";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TsError(
            @JsonProperty("tsErrorCode")
            String tsErrorCode,

            @JsonProperty("tsErrorLevel")
            String tsErrorLevel
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseBody(
            @JsonProperty("entities")
            Entities entities
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entities(
            @JsonProperty("total")
            String total,

            @JsonProperty("entity")
            Entity entity
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entity(
            @JsonProperty("CODPARC")
            SankhyaField codParc,

            @JsonProperty("NOMEPARC")
            SankhyaField nomeparc,

            @JsonProperty("CGC_CPF")
            SankhyaField cgcCpf
    ) {}
}

