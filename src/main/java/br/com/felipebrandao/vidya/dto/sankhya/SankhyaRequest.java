package br.com.felipebrandao.vidya.dto.sankhya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SankhyaRequest(

        @JsonProperty("serviceName")
        String serviceName,

        @JsonProperty("requestBody")
        RequestBody requestBody
) {

    public record RequestBody(
            @JsonProperty("dataSet")
            DataSet dataSet
    ) {}

    public record DataSet(
            @JsonProperty("rootEntity") String rootEntity,
            @JsonProperty("includePresentationFields") String includePresentationFields,
            @JsonProperty("offsetPage") String offsetPage,
            @JsonProperty("criteria") Criteria criteria,
            @JsonProperty("dataRow") DataRow dataRow,
            @JsonProperty("entity") Entity entity
    ) {}

    public record Criteria(
            @JsonProperty("expression") SankhyaField expression
    ) {}

    public record DataRow(
            @JsonProperty("localFields") Object localFields
    ) {}

    public record Entity(
            @JsonProperty("fieldset") Fieldset fieldset
    ) {}

    public record Fieldset(
            @JsonProperty("list") String list
    ) {}
}

