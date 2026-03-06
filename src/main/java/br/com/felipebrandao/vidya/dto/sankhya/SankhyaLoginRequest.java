package br.com.felipebrandao.vidya.dto.sankhya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SankhyaLoginRequest(

        @JsonProperty("serviceName")
        String serviceName,

        @JsonProperty("requestBody")
        RequestBody requestBody
) {

    public record RequestBody(
            @JsonProperty("NOMUSU") SankhyaField nomusu,
            @JsonProperty("INTERNO") SankhyaField interno,
            @JsonProperty("KEEPCONNECTED") SankhyaField keepConnected
    ) {}

    public static SankhyaLoginRequest of(String username, String password) {
        return new SankhyaLoginRequest(
                "MobileLoginSP.login",
                new RequestBody(
                        new SankhyaField(username),
                        new SankhyaField(password),
                        new SankhyaField("N")
                )
        );
    }
}

