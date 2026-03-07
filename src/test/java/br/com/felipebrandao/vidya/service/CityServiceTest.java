package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.client.SankhyaClient;
import br.com.felipebrandao.vidya.dto.response.CityResponse;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaField;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaResponse;
import br.com.felipebrandao.vidya.entity.City;
import br.com.felipebrandao.vidya.exception.IntegrationException;
import br.com.felipebrandao.vidya.mapper.CityMapper;
import br.com.felipebrandao.vidya.repository.CityBatchRepository;
import br.com.felipebrandao.vidya.repository.CityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CityBatchRepository cityBatchRepository;

    @Mock
    private CityMapper cityMapper;

    @Mock
    private SankhyaClient sankhyaClient;

    @Mock
    private SankhyaAuthService sankhyaAuthService;

    @InjectMocks
    private CityService cityService;

    @Test
    @DisplayName("list: deve sincronizar uma página e retornar cidades mapeadas do banco")
    void givenOnPageOfCitiesWhenListShouldBatchUpsertAndReturnFromDb() {
        SankhyaField codcid   = new SankhyaField("100");
        SankhyaField nomeCid  = new SankhyaField("São Paulo");
        SankhyaField estadoUF = new SankhyaField("SP");

        SankhyaResponse.EntityRecord record = new SankhyaResponse.EntityRecord(codcid, nomeCid, null, estadoUF, null, null, null);
        SankhyaResponse.Entities entities = new SankhyaResponse.Entities("1", "false", "0", List.of(record));
        SankhyaResponse sankhyaResponse = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(entities));

        City city = City.builder().id(1L).codcid(100).nome("São Paulo").uf("SP").build();
        CityResponse cityResponse = new CityResponse(1L, 100, "São Paulo", "SP");

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any())).thenReturn(sankhyaResponse);
        when(cityRepository.findAll()).thenReturn(List.of(city));
        when(cityMapper.toResponseList(List.of(city))).thenReturn(List.of(cityResponse));

        List<CityResponse> result = cityService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nome()).isEqualTo("São Paulo");
        assertThat(result.get(0).uf()).isEqualTo("SP");
        verify(cityBatchRepository).batchUpsert(any());
        verify(cityRepository).findAll();
    }

    @Test
    @DisplayName("list: deve percorrer múltiplas páginas até hasMoreResult ser false")
    void givenMultiplePagesWhenListShouldFetchAllPages() {
        SankhyaField cod1 = new SankhyaField("1");
        SankhyaField nom1 = new SankhyaField("Cidade A");
        SankhyaField uf1  = new SankhyaField("SP");
        SankhyaResponse.EntityRecord rec1 = new SankhyaResponse.EntityRecord(cod1, nom1, null, uf1, null, null, null);

        SankhyaField cod2 = new SankhyaField("2");
        SankhyaField nom2 = new SankhyaField("Cidade B");
        SankhyaField uf2  = new SankhyaField("RJ");
        SankhyaResponse.EntityRecord rec2 = new SankhyaResponse.EntityRecord(cod2, nom2, null, uf2, null, null, null);

        SankhyaResponse page0 = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(
                        new SankhyaResponse.Entities("2", "true", "0", List.of(rec1))));
        SankhyaResponse page1 = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(
                        new SankhyaResponse.Entities("2", "false", "1", List.of(rec2))));

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any()))
                .thenReturn(page0)
                .thenReturn(page1);
        when(cityRepository.findAll()).thenReturn(List.of());
        when(cityMapper.toResponseList(any())).thenReturn(List.of());

        cityService.list();

        verify(sankhyaClient, times(2)).loadRecords(anyString(), any());
        verify(cityBatchRepository, times(2)).batchUpsert(any());
    }

    @Test
    @DisplayName("list: deve renovar sessão e tentar novamente quando Sankhya retorna falha")
    void givenExpiredSessionWhenListShouldRenewAndRetry() {
        SankhyaResponse failResponse = new SankhyaResponse("loadRecords", "0", null);

        SankhyaField cod      = new SankhyaField("5");
        SankhyaField nom      = new SankhyaField("Recife");
        SankhyaField estadoUF = new SankhyaField("PE");
        SankhyaResponse.EntityRecord record = new SankhyaResponse.EntityRecord(cod, nom, null, estadoUF, null, null, null);
        SankhyaResponse successResponse = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(
                        new SankhyaResponse.Entities("1", "false", "0", List.of(record))));

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=expired");
        when(sankhyaAuthService.renewSessionCookie()).thenReturn("JSESSIONID=new");
        when(sankhyaClient.loadRecords(anyString(), any()))
                .thenReturn(failResponse)
                .thenReturn(successResponse);
        when(cityRepository.findAll()).thenReturn(List.of());
        when(cityMapper.toResponseList(any())).thenReturn(List.of());

        cityService.list();

        verify(sankhyaAuthService).renewSessionCookie();
        verify(sankhyaClient, times(2)).loadRecords(anyString(), any());
    }

    @Test
    @DisplayName("list: deve lançar IntegrationException quando Sankhya lança exceção inesperada")
    void givenSankhyaThrowsExceptionWhenListShouldThrowIntegrationException() {
        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> cityService.list())
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Falha ao buscar cidades no Sankhya");
    }

    @Test
    @DisplayName("list: deve interromper sincronização quando Sankhya retorna lista de cidades nula")
    void givenNullEntityListWhenListShouldStopSyncAndReturnDbData() {
        SankhyaResponse emptyResponse = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(
                        new SankhyaResponse.Entities("0", "false", "0", null)));

        City city = City.builder().id(1L).codcid(1).nome("Cached City").uf("SP").build();
        CityResponse cityResponse = new CityResponse(1L, 1, "Cached City", "SP");

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any())).thenReturn(emptyResponse);
        when(cityRepository.findAll()).thenReturn(List.of(city));
        when(cityMapper.toResponseList(List.of(city))).thenReturn(List.of(cityResponse));

        List<CityResponse> result = cityService.list();

        assertThat(result).hasSize(1);
        verify(cityBatchRepository, never()).batchUpsert(any());
    }

    @Test
    @DisplayName("list: deve ignorar registros com codcid ou nome nulos no batchUpsert")
    void givenRecordsWithNullCodeWhenListShouldIgnoreThem() {
        SankhyaResponse.EntityRecord validRecord = new SankhyaResponse.EntityRecord(
                new SankhyaField("10"), new SankhyaField("Porto Alegre"),
                null, new SankhyaField("RS"), null, null, null);

        SankhyaResponse.EntityRecord invalidRecord = new SankhyaResponse.EntityRecord(
                null, new SankhyaField("Sem Código"),
                null, new SankhyaField("XX"), null, null, null);

        SankhyaResponse.Entities entities = new SankhyaResponse.Entities("2", "false", "0",
                List.of(validRecord, invalidRecord));
        SankhyaResponse sankhyaResponse = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(entities));

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any())).thenReturn(sankhyaResponse);
        when(cityRepository.findAll()).thenReturn(List.of());
        when(cityMapper.toResponseList(any())).thenReturn(List.of());

        cityService.list();

        verify(cityBatchRepository).batchUpsert(
                org.mockito.ArgumentMatchers.argThat(rows -> rows.size() == 1));
    }
}



