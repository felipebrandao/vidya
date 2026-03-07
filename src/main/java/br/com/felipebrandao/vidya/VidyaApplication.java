package br.com.felipebrandao.vidya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

import br.com.felipebrandao.vidya.config.SankhyaProperties;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(SankhyaProperties.class)
public class VidyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(VidyaApplication.class, args);
    }

}
