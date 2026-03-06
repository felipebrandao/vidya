package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.client.SankhyaClient;
import br.com.felipebrandao.vidya.dto.response.CityResponse;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaField;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaRequest;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaResponse;
import br.com.felipebrandao.vidya.exception.IntegrationException;
import br.com.felipebrandao.vidya.mapper.CityMapper;
import br.com.felipebrandao.vidya.repository.CityBatchRepository;
import br.com.felipebrandao.vidya.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CityBatchRepository cityBatchRepository;
    private final CityMapper cityMapper;
    private final SankhyaClient sankhyaClient;
    private final SankhyaAuthService sankhyaAuthService;

    @Transactional
    public List<CityResponse> list() {
        log.info("Sincronizando cidades com o Sankhya...");
        syncCities();
        return cityMapper.toResponseList(cityRepository.findAll());
    }

    private void syncCities() {
        try {
            String cookie = sankhyaAuthService.getSessionCookie();
            int page = 0;
            int totalSynced = 0;

            do {
                SankhyaRequest request = buildListCitiesRequest(String.valueOf(page));
                SankhyaResponse response = sankhyaClient.loadRecords(cookie, request);

                if (!response.isSuccess()) {
                    cookie = sankhyaAuthService.renewSessionCookie();
                    response = sankhyaClient.loadRecords(cookie, request);
                }

                if (response.responseBody() == null
                        || response.responseBody().entities() == null
                        || response.responseBody().entities().entity() == null) {
                    log.warn("Sankhya retornou lista de cidades vazia na página {}", page);
                    break;
                }

                List<SankhyaResponse.EntityRecord> records = response.responseBody().entities().entity();
                log.info("Página {}: recebidas {} cidades do Sankhya", page, records.size());

                int upserted = batchUpsertPage(records);
                totalSynced += upserted;

                if (!response.responseBody().entities().hasMore()) {
                    break;
                }

                page++;

            } while (true);

            log.info("Sincronização de cidades concluída: {} cidades processadas", totalSynced);

        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao sincronizar cidades com Sankhya: {}", e.getMessage());
            throw new IntegrationException("Falha ao buscar cidades no Sankhya", e);
        }
    }

    private int batchUpsertPage(List<SankhyaResponse.EntityRecord> cities) {
        List<Object[]> rows = new ArrayList<>();

        for (SankhyaResponse.EntityRecord city : cities) {
            String codcidStr = city.f0() != null ? city.f0().value() : null;
            String nomecid   = city.f1() != null ? city.f1().value() : null;
            String uf        = city.f3() != null ? city.f3().value() : null;

            if (codcidStr == null || nomecid == null) continue;

            rows.add(new Object[]{Integer.parseInt(codcidStr), nomecid, uf});
        }

        cityBatchRepository.batchUpsert(rows);
        return rows.size();
    }

    private SankhyaRequest buildListCitiesRequest(String offsetPage) {
        return new SankhyaRequest(
                "CRUDServiceProvider.loadRecords",
                new SankhyaRequest.RequestBody(
                        new SankhyaRequest.DataSet(
                                "Cidade",
                                "S",
                                offsetPage,
                                new SankhyaRequest.Criteria(new SankhyaField("1=1")),
                                null,
                                new SankhyaRequest.Entity(
                                        new SankhyaRequest.Fieldset("CODCID, NOMECID, UF")
                                )
                        )
                )
        );
    }
}
