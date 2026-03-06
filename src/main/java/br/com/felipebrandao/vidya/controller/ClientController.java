package br.com.felipebrandao.vidya.controller;

import br.com.felipebrandao.vidya.config.OpenApiConfig;
import br.com.felipebrandao.vidya.dto.request.ClientRequest;
import br.com.felipebrandao.vidya.dto.response.ClientResponse;
import br.com.felipebrandao.vidya.dto.response.ReceitaWSResponse;
import br.com.felipebrandao.vidya.service.ClientService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> list() {
        return ResponseEntity.ok(clientService.list());
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<ReceitaWSResponse> lookupCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(clientService.lookupCnpj(cnpj));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }
}

