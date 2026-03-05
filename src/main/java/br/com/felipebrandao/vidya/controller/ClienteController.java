package br.com.felipebrandao.vidya.controller;

import br.com.felipebrandao.vidya.config.OpenApiConfig;
import br.com.felipebrandao.vidya.dto.request.ClienteRequest;
import br.com.felipebrandao.vidya.dto.response.ClienteResponse;
import br.com.felipebrandao.vidya.dto.response.ReceitaWSResponse;
import br.com.felipebrandao.vidya.service.ClienteService;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<ReceitaWSResponse> consultarCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(clienteService.consultarCnpj(cnpj));
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.cadastrar(request));
    }
}

