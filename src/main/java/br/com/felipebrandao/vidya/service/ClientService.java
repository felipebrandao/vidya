package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.client.ReceitaWSClient;
import br.com.felipebrandao.vidya.client.SankhyaClient;
import br.com.felipebrandao.vidya.dto.request.ClientRequest;
import br.com.felipebrandao.vidya.dto.response.ClientResponse;
import br.com.felipebrandao.vidya.dto.response.ReceitaWSResponse;
import br.com.felipebrandao.vidya.dto.sankhya.ParceiroLocalFields;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaField;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaRequest;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaResponse;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaSaveResponse;
import br.com.felipebrandao.vidya.entity.Client;
import br.com.felipebrandao.vidya.entity.PersonType;
import br.com.felipebrandao.vidya.exception.DuplicateResourceException;
import br.com.felipebrandao.vidya.exception.IntegrationException;
import br.com.felipebrandao.vidya.mapper.ClientMapper;
import br.com.felipebrandao.vidya.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ReceitaWSClient receitaWSClient;
    private final SankhyaClient sankhyaClient;
    private final SankhyaAuthService sankhyaAuthService;

    @Transactional
    public List<ClientResponse> list() {
        log.info("Sincronizando clientes com o Sankhya...");
        syncClients();
        return clientMapper.toResponseList(clientRepository.findAllByOrderByNomeAsc());
    }

    public ReceitaWSResponse lookupCnpj(String cnpj) {
        String cleanCnpj = cnpj.replaceAll("\\D", "");
        log.info("Consultando CNPJ {} na ReceitaWS", cleanCnpj);

        try {
            ReceitaWSResponse response = receitaWSClient.lookupCnpj(cleanCnpj);

            if (response == null || "ERROR".equalsIgnoreCase(response.status())) {
                throw new IntegrationException("CNPJ não encontrado ou inválido: " + cleanCnpj);
            }

            return response;
        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao consultar CNPJ {} na ReceitaWS: {}", cleanCnpj, e.getMessage());
            throw new IntegrationException("Falha ao consultar CNPJ na ReceitaWS", e);
        }
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        log.info("Cadastrando cliente com CNPJ/CPF: {}", request.cgcCpf());

        if (clientRepository.existsByCgcCpf(request.cgcCpf())) {
            log.warn("Cliente com CNPJ/CPF {} já existe na base local", request.cgcCpf());
            throw new DuplicateResourceException("Cliente", "CNPJ/CPF", request.cgcCpf());
        }

        Client client = clientMapper.toEntity(request);
        Client saved = clientRepository.save(client);
        log.info("Cliente id={} salvo localmente", saved.getId());

        try {
            String cookie = sankhyaAuthService.getSessionCookie();
            SankhyaRequest sankhyaRequest = buildSavePartnerRequest(request);
            SankhyaSaveResponse response = sankhyaClient.saveRecord(cookie, sankhyaRequest);

            if (!response.isSuccess()) {
                cookie = sankhyaAuthService.renewSessionCookie();
                response = sankhyaClient.saveRecord(cookie, sankhyaRequest);
            }

            if (!response.isSuccess()) {
                log.warn("Sankhya recusou o cadastro do cliente id={}: {}", saved.getId(), response.errorMessage());
                clientRepository.delete(saved);
                throw new IntegrationException("Sankhya recusou o cadastro: " + response.errorMessage());
            }

            Integer codParc = extractPartnerCode(response);
            if (codParc != null) {
                saved.setCodSankhya(codParc);
                clientRepository.save(saved);
                log.info("Cliente id={} atualizado com CODPARC={} do Sankhya", saved.getId(), codParc);
            }

        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na integração Sankhya para cliente id={}: {}", saved.getId(), e.getMessage());
            clientRepository.delete(saved);
            throw new IntegrationException("Falha na comunicação com o Sankhya", e);
        }

        return clientMapper.toResponse(saved);
    }

    private void syncClients() {
        try {
            String cookie = sankhyaAuthService.getSessionCookie();
            int page = 0;
            int totalSynced = 0;

            do {
                SankhyaRequest request = buildListClientsRequest(String.valueOf(page));
                SankhyaResponse response = sankhyaClient.loadRecords(cookie, request);

                if (!response.isSuccess()) {
                    cookie = sankhyaAuthService.renewSessionCookie();
                    response = sankhyaClient.loadRecords(cookie, request);
                }

                if (response.responseBody() == null
                        || response.responseBody().entities() == null
                        || response.responseBody().entities().entity() == null) {
                    log.warn("Sankhya retornou lista de clientes vazia na página {}", page);
                    break;
                }

                List<SankhyaResponse.EntityRecord> clients = response.responseBody().entities().entity();
                log.info("Página {}: recebidos {} clientes do Sankhya", page, clients.size());

                for (SankhyaResponse.EntityRecord client : clients) {
                    processClientRecord(client);
                    totalSynced++;
                }

                if (!response.responseBody().entities().hasMore()) {
                    break;
                }

                page++;

            } while (true);

            log.info("Sincronização concluída: {} clientes processados", totalSynced);

        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao sincronizar clientes com Sankhya: {}", e.getMessage());
            throw new IntegrationException("Falha ao buscar clientes no Sankhya", e);
        }
    }

    private void processClientRecord(SankhyaResponse.EntityRecord client) {
        String codparcStr  = client.f0() != null ? client.f0().value() : null;
        String cgcCpf      = client.f1() != null ? client.f1().value() : null;
        String nomeparc    = client.f2() != null ? client.f2().value() : "";
        String razaoSocial = client.f3() != null ? client.f3().value() : null;
        String tipPessoaStr = client.f4() != null ? client.f4().value() : "J";
        String classificMs = client.f5() != null ? client.f5().value() : null;
        String codcidStr   = client.f6() != null ? client.f6().value() : "0";

        if (codparcStr == null || nomeparc.isBlank()) return;

        Integer codParc = Integer.parseInt(codparcStr);
        PersonType tipPessoa = "F".equalsIgnoreCase(tipPessoaStr) ? PersonType.F : PersonType.J;
        int codcid = parseCodcid(codcidStr);

        clientRepository.findByCodSankhya(codParc)
                .ifPresentOrElse(
                        existing -> {
                            existing.setCgcCpf(cgcCpf);
                            existing.setNomeparc(nomeparc);
                            existing.setRazaoSocial(razaoSocial);
                            existing.setTipPessoa(tipPessoa);
                            existing.setClassificMs(classificMs);
                            existing.setCodcid(codcid);
                            clientRepository.save(existing);
                        },
                        () -> clientRepository.save(
                                Client.builder()
                                        .nome(nomeparc)
                                        .cgcCpf(cgcCpf)
                                        .nomeparc(nomeparc)
                                        .razaoSocial(razaoSocial)
                                        .codcid(codcid)
                                        .tipPessoa(tipPessoa)
                                        .classificMs(classificMs)
                                        .codSankhya(codParc)
                                        .build()
                        )
                );
    }

    private int parseCodcid(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private SankhyaRequest buildSavePartnerRequest(ClientRequest req) {
        ParceiroLocalFields fields = new ParceiroLocalFields(
                new SankhyaField(req.cgcCpf()),
                new SankhyaField(req.nomeparc()),
                new SankhyaField(req.razaoSocial()),
                new SankhyaField(req.tipPessoa().name()),
                new SankhyaField(req.classificMs()),
                new SankhyaField(String.valueOf(req.codcid()))
        );

        return new SankhyaRequest(
                "CRUDServiceProvider.saveRecord",
                new SankhyaRequest.RequestBody(
                        new SankhyaRequest.DataSet(
                                "Parceiro",
                                "S",
                                null,
                                null,
                                new SankhyaRequest.DataRow(fields),
                                new SankhyaRequest.Entity(
                                        new SankhyaRequest.Fieldset("CODPARC, NOMEPARC, CGC_CPF")
                                )
                        )
                )
        );
    }

    private SankhyaRequest buildListClientsRequest(String offsetPage) {
        return new SankhyaRequest(
                "CRUDServiceProvider.loadRecords",
                new SankhyaRequest.RequestBody(
                        new SankhyaRequest.DataSet(
                                "Parceiro",
                                "S",
                                offsetPage,
                                new SankhyaRequest.Criteria(new SankhyaField("1=1")),
                                null,
                                new SankhyaRequest.Entity(
                                        new SankhyaRequest.Fieldset("CODPARC, CGC_CPF, NOMEPARC, RAZAOSOCIAL, TIPPESSOA, CLASSIFICMS, CODCID")
                                )
                        )
                )
        );
    }

    private Integer extractPartnerCode(SankhyaSaveResponse response) {
        try {
            if (response.responseBody() != null
                    && response.responseBody().entities() != null
                    && response.responseBody().entities().entity() != null) {

                SankhyaField codParc = response.responseBody().entities().entity().codParc();
                if (codParc != null && codParc.value() != null) {
                    return Integer.parseInt(codParc.value());
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível extrair CODPARC da resposta Sankhya: {}", e.getMessage());
        }
        return null;
    }
}
