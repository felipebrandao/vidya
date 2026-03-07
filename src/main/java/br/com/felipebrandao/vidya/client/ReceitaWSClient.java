package br.com.felipebrandao.vidya.client;

import br.com.felipebrandao.vidya.dto.response.ReceitaWSResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "receitaws", url = "${receitaws.url:https://receitaws.com.br/v1}")
public interface ReceitaWSClient {

    @GetMapping("/cnpj/{cnpj}")
    ReceitaWSResponse lookupCnpj(@PathVariable("cnpj") String cnpj);
}

