package br.com.felipebrandao.vidya.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Configuration
public class FeignConfig {

    @Bean
    public Decoder feignDecoder(ObjectMapper objectMapper) {
        return (response, type) -> {
            Charset charset = StandardCharsets.UTF_8;

            if (response.headers().containsKey("content-type")) {
                String contentType = response.headers().get("content-type")
                        .stream().findFirst().orElse("");
                if (contentType.toLowerCase().contains("charset=")) {
                    String charsetName = contentType.toLowerCase()
                            .replaceAll(".*charset=([^;\\s]+).*", "$1")
                            .trim();
                    try {
                        charset = Charset.forName(charsetName);
                    } catch (Exception ignored) {
                        // mantém UTF-8
                    }
                }
            }

            try (feign.Response.Body body = response.body()) {
                if (body == null) return null;
                byte[] bytes = body.asInputStream().readAllBytes();
                String json = new String(bytes, charset);
                return objectMapper.readValue(json, objectMapper.constructType(type));
            } catch (IOException e) {
                throw new feign.codec.DecodeException(
                        response.status(),
                        "Erro ao decodificar resposta do Sankhya: " + e.getMessage(),
                        response.request(),
                        e
                );
            }
        };
    }
}


