package br.com.felipebrandao.vidya.dto.sankhya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SankhyaResponse(

        @JsonProperty("serviceName")
        String serviceName,

        @JsonProperty("status")
        String status,

        @JsonProperty("responseBody")
        ResponseBody responseBody
) {

    public boolean isSuccess() {
        return "1".equals(status);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseBody(
            @JsonProperty("entities")
            Entities entities
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entities(
            @JsonProperty("total")
            String total,

            @JsonProperty("hasMoreResult")
            String hasMoreResult,

            @JsonProperty("offsetPage")
            String offsetPage,

            @JsonProperty("entity")
            List<EntityRecord> entity
    ) {
        public boolean hasMore() {
            return "true".equalsIgnoreCase(hasMoreResult);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EntityRecord(
            @JsonProperty("f0") SankhyaField f0,
            @JsonProperty("f1") SankhyaField f1,
            @JsonProperty("f2") SankhyaField f2,
            @JsonProperty("f3") SankhyaField f3,
            @JsonProperty("f4") SankhyaField f4,
            @JsonProperty("f5") SankhyaField f5,
            @JsonProperty("f6") SankhyaField f6
    ) {}
}

