package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.client.ReceitaWSClient;
import br.com.felipebrandao.vidya.dto.request.ClienteRequest;
import br.com.felipebrandao.vidya.dto.response.ClienteResponse;
import br.com.felipebrandao.vidya.dto.response.ReceitaWSResponse;
import br.com.felipebrandao.vidya.entity.Cliente;
import br.com.felipebrandao.vidya.exception.IntegrationException;
import br.com.felipebrandao.vidya.mapper.ClienteMapper;
import br.com.felipebrandao.vidya.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final ReceitaWSClient receitaWSClient;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteMapper.toResponseList(clienteRepository.findAllByOrderByNomeAsc());
    }

    public ReceitaWSResponse consultarCnpj(String cnpj) {
        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");
        log.info("Consultando CNPJ {} na ReceitaWS", cnpjLimpo);

        try {
            ReceitaWSResponse response = receitaWSClient.consultarCnpj(cnpjLimpo);

            if (response == null || "ERROR".equalsIgnoreCase(response.status())) {
                throw new IntegrationException("CNPJ não encontrado ou inválido: " + cnpjLimpo);
            }

            return response;
        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao consultar CNPJ {} na ReceitaWS: {}", cnpjLimpo, e.getMessage());
            throw new IntegrationException("Falha ao consultar CNPJ na ReceitaWS", e);
        }
    }

    @Transactional
    public ClienteResponse cadastrar(ClienteRequest request) {
        log.info("Cadastrando cliente com CNPJ/CPF: {}", request.cgcCpf());

        Cliente cliente = clienteMapper.toEntity(request);

        Cliente salvo = clienteRepository.save(cliente);
        log.info("Cliente id={} salvo localmente", salvo.getId());

        return clienteMapper.toResponse(salvo);
    }
}

