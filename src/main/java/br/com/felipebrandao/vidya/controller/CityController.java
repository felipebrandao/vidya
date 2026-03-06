package br.com.felipebrandao.vidya.controller;

import br.com.felipebrandao.vidya.config.OpenApiConfig;
import br.com.felipebrandao.vidya.dto.response.CityResponse;
import br.com.felipebrandao.vidya.service.CityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<List<CityResponse>> list() {
        return ResponseEntity.ok(cityService.list());
    }
}

