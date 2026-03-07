package br.com.felipebrandao.vidya.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sankhya")
public class SankhyaProperties {

    private String baseUrl;
    private String username;
    private String password;
}

