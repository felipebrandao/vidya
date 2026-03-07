package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.client.SankhyaClient;
import br.com.felipebrandao.vidya.config.SankhyaProperties;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaLoginRequest;
import br.com.felipebrandao.vidya.exception.IntegrationException;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class SankhyaAuthService {

    private final SankhyaClient sankhyaClient;
    private final SankhyaProperties sankhyaProperties;

    private final AtomicReference<String> jsessionid = new AtomicReference<>();

    public String getSessionCookie() {
        if (jsessionid.get() == null) {
            synchronized (this) {
                if (jsessionid.get() == null) login();
            }
        }
        return "JSESSIONID=" + jsessionid.get();
    }

    public String renewSessionCookie() {
        log.info("Renovando sessão Sankhya...");
        jsessionid.set(null);
        return getSessionCookie();
    }

    public void resetSession() {
        jsessionid.set(null);
    }

    private void login() {
        log.info("Realizando login no Sankhya com usuário: {}", sankhyaProperties.getUsername());

        try {
            SankhyaLoginRequest loginRequest = SankhyaLoginRequest.of(
                    sankhyaProperties.getUsername(),
                    sankhyaProperties.getPassword()
            );

            Response response = sankhyaClient.login(loginRequest);

            String extractedId = extractJsessionid(response.headers());

            if (extractedId == null) {
                throw new IntegrationException("Login no Sankhya não retornou jsessionid");
            }

            jsessionid.set(extractedId);
            log.info("Login no Sankhya realizado com sucesso");

        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao realizar login no Sankhya: {}", e.getMessage());
            throw new IntegrationException("Falha ao autenticar no Sankhya", e);
        }
    }

    private String extractJsessionid(Map<String, Collection<String>> headers) {
        Collection<String> setCookieHeaders = headers.get("set-cookie");

        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
            return null;
        }

        for (String cookie : setCookieHeaders) {
            if (cookie.startsWith("JSESSIONID=")) {
                return cookie.split(";")[0].replace("JSESSIONID=", "").trim();
            }
        }

        return null;
    }
}

