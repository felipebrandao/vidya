package br.com.felipebrandao.vidya.client;

import br.com.felipebrandao.vidya.config.FeignConfig;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaLoginRequest;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaRequest;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaResponse;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaSaveResponse;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "sankhya", url = "${sankhya.base-url}", configuration = FeignConfig.class)
public interface SankhyaClient {

    @PostMapping(value = "/mge/service.sbr", params = {"serviceName=MobileLoginSP.login", "outputType=json"})
    Response login(@RequestBody SankhyaLoginRequest request);

    @PostMapping(value = "/mge/service.sbr", params = {"serviceName=CRUDServiceProvider.loadRecords", "outputType=json"})
    SankhyaResponse loadRecords(
            @RequestHeader("Cookie") String cookie,
            @RequestBody SankhyaRequest request
    );

    @PostMapping(value = "/mge/service.sbr", params = {"serviceName=CRUDServiceProvider.saveRecord", "outputType=json"})
    SankhyaSaveResponse saveRecord(
            @RequestHeader("Cookie") String cookie,
            @RequestBody SankhyaRequest request
    );
}

