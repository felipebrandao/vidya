package br.com.felipebrandao.vidya.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sankhya")
public class SankhyaProperties {

    private String baseUrl;
    private String username;
    private String password;
}

