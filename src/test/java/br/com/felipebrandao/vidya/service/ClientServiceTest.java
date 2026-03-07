package br.com.felipebrandao.vidya.service;

import br.com.felipebrandao.vidya.client.ReceitaWSClient;
import br.com.felipebrandao.vidya.client.SankhyaClient;
import br.com.felipebrandao.vidya.dto.request.ClientRequest;
import br.com.felipebrandao.vidya.dto.response.ClientResponse;
import br.com.felipebrandao.vidya.dto.response.ReceitaWSResponse;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaField;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaResponse;
import br.com.felipebrandao.vidya.dto.sankhya.SankhyaSaveResponse;
import br.com.felipebrandao.vidya.entity.Client;
import br.com.felipebrandao.vidya.entity.PersonType;
import br.com.felipebrandao.vidya.exception.DuplicateResourceException;
import br.com.felipebrandao.vidya.exception.IntegrationException;
import br.com.felipebrandao.vidya.mapper.ClientMapper;
import br.com.felipebrandao.vidya.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private ReceitaWSClient receitaWSClient;

    @Mock
    private SankhyaClient sankhyaClient;

    @Mock
    private SankhyaAuthService sankhyaAuthService;

    @InjectMocks
    private ClientService clientService;

    @Test
    @DisplayName("list: deve sincronizar com Sankhya e retornar lista do banco")
    void givenSankhyaWithOnePageWhenListShouldReturnMappedClients() {
        SankhyaField f0 = new SankhyaField("10");
        SankhyaField f1 = new SankhyaField("12345678000100");
        SankhyaField f2 = new SankhyaField("Empresa X");
        SankhyaField f3 = new SankhyaField("Empresa X Ltda");
        SankhyaField f4 = new SankhyaField("J");
        SankhyaField f5 = new SankhyaField("C");
        SankhyaField f6 = new SankhyaField("1");

        SankhyaResponse.EntityRecord record = new SankhyaResponse.EntityRecord(f0, f1, f2, f3, f4, f5, f6);
        SankhyaResponse.Entities pageEntities = new SankhyaResponse.Entities("1", "false", "0", List.of(record));
        SankhyaResponse.ResponseBody body = new SankhyaResponse.ResponseBody(pageEntities);
        SankhyaResponse sankhyaResponse = new SankhyaResponse("loadRecords", "1", body);

        Client client = Client.builder().id(1L).nome("Empresa X").cgcCpf("12345678000100").build();
        ClientResponse clientResponse = new ClientResponse(1L, "Empresa X", "12345678000100",
                "Empresa X", "Empresa X Ltda", 1, PersonType.J, "C", 10, null, null);

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any())).thenReturn(sankhyaResponse);
        when(clientRepository.findByCodSankhya(10)).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(client));
        when(clientMapper.toResponseList(List.of(client))).thenReturn(List.of(clientResponse));

        List<ClientResponse> result = clientService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nome()).isEqualTo("Empresa X");
        verify(sankhyaClient).loadRecords(anyString(), any());
        verify(clientRepository).findAllByOrderByNomeAsc();
    }

    @Test
    @DisplayName("list: deve atualizar cliente existente quando codSankhya já existe na base")
    void givenExistingClientWhenListShouldUpdateInsteadOfInsert() {
        SankhyaField f0 = new SankhyaField("10");
        SankhyaField f1 = new SankhyaField("12345678000100");
        SankhyaField f2 = new SankhyaField("Empresa Y");
        SankhyaResponse.EntityRecord record = new SankhyaResponse.EntityRecord(f0, f1, f2, null,
                new SankhyaField("J"), null, new SankhyaField("1"));

        SankhyaResponse.Entities pageEntities = new SankhyaResponse.Entities("1", "false", "0", List.of(record));
        SankhyaResponse sankhyaResponse = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(pageEntities));

        Client existing = Client.builder().id(5L).codSankhya(10).nome("Old Name").build();

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any())).thenReturn(sankhyaResponse);
        when(clientRepository.findByCodSankhya(10)).thenReturn(Optional.of(existing));
        when(clientRepository.save(any(Client.class))).thenReturn(existing);
        when(clientRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(existing));
        when(clientMapper.toResponseList(any())).thenReturn(List.of());

        clientService.list();

        verify(clientRepository, times(1)).save(existing);
        verify(clientRepository, never()).save(
                org.mockito.ArgumentMatchers.argThat(c -> c != existing && c.getId() == null));
    }

    @Test
    @DisplayName("list: deve renovar sessão e tentar novamente quando Sankhya retorna falha")
    void givenExpiredSessionWhenListShouldRenewAndRetry() {
        SankhyaResponse failResponse = new SankhyaResponse("loadRecords", "0", null);

        SankhyaField f0 = new SankhyaField("10");
        SankhyaField f2 = new SankhyaField("Client Z");
        SankhyaResponse.EntityRecord record = new SankhyaResponse.EntityRecord(f0, null, f2, null,
                new SankhyaField("J"), null, new SankhyaField("0"));
        SankhyaResponse.Entities pageEntities = new SankhyaResponse.Entities("1", "false", "0", List.of(record));
        SankhyaResponse successResponse = new SankhyaResponse("loadRecords", "1",
                new SankhyaResponse.ResponseBody(pageEntities));

        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=expired");
        when(sankhyaAuthService.renewSessionCookie()).thenReturn("JSESSIONID=new");
        when(sankhyaClient.loadRecords(anyString(), any()))
                .thenReturn(failResponse)
                .thenReturn(successResponse);
        when(clientRepository.findByCodSankhya(10)).thenReturn(Optional.empty());
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findAllByOrderByNomeAsc()).thenReturn(List.of());
        when(clientMapper.toResponseList(any())).thenReturn(List.of());

        clientService.list();

        verify(sankhyaAuthService).renewSessionCookie();
        verify(sankhyaClient, times(2)).loadRecords(anyString(), any());
    }

    @Test
    @DisplayName("list: deve lançar IntegrationException quando Sankhya lança exceção inesperada")
    void givenSankhyaThrowsExceptionWhenListShouldThrowIntegrationException() {
        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.loadRecords(anyString(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> clientService.list())
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Falha ao buscar clientes no Sankhya");
    }

    @Test
    @DisplayName("lookupCnpj: deve limpar formatação e retornar resposta da ReceitaWS")
    void givenFormattedCnpjWhenLookupShouldStripAndReturnResponse() {
        ReceitaWSResponse receitaResponse = new ReceitaWSResponse(
                "OK", "12345678000100", "Empresa X", null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        when(receitaWSClient.lookupCnpj("12345678000100")).thenReturn(receitaResponse);

        ReceitaWSResponse result = clientService.lookupCnpj("12.345.678/0001-00");

        assertThat(result.cnpj()).isEqualTo("12345678000100");
        verify(receitaWSClient).lookupCnpj("12345678000100");
    }

    @Test
    @DisplayName("lookupCnpj: deve lançar IntegrationException quando status for ERROR")
    void givenErrorStatusWhenLookupShouldThrowIntegrationException() {
        ReceitaWSResponse errorResponse = new ReceitaWSResponse(
                "ERROR", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        when(receitaWSClient.lookupCnpj("00000000000000")).thenReturn(errorResponse);

        assertThatThrownBy(() -> clientService.lookupCnpj("00000000000000"))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("00000000000000");
    }

    @Test
    @DisplayName("lookupCnpj: deve lançar IntegrationException quando ReceitaWS lança exceção")
    void givenReceitaWSThrowsExceptionWhenLookupShouldThrowIntegrationException() {
        when(receitaWSClient.lookupCnpj(anyString()))
                .thenThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> clientService.lookupCnpj("12345678000100"))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Falha ao consultar CNPJ na ReceitaWS");
    }

    @Test
    @DisplayName("lookupCnpj: deve lançar IntegrationException quando resposta for nula")
    void givenNullResponseWhenLookupShouldThrowIntegrationException() {
        when(receitaWSClient.lookupCnpj(anyString())).thenReturn(null);

        assertThatThrownBy(() -> clientService.lookupCnpj("12345678000100"))
                .isInstanceOf(IntegrationException.class);
    }

    @Test
    @DisplayName("create: deve salvar localmente e enviar ao Sankhya, retornando ClientResponse")
    void givenNewClientWhenCreateShouldSaveAndSendToSankhya() {
        ClientRequest request = new ClientRequest("Emp X", "12345678000100",
                "Emp X", "Emp X Ltda", 1, PersonType.J, "C");

        Client clientEntity = Client.builder().id(1L).cgcCpf("12345678000100").nome("Emp X").build();
        ClientResponse clientResponse = new ClientResponse(1L, "Emp X", "12345678000100",
                "Emp X", "Emp X Ltda", 1, PersonType.J, "C", null, null, null);

        SankhyaSaveResponse.Entity entity = new SankhyaSaveResponse.Entity(
                new SankhyaField("42"), new SankhyaField("Emp X"), new SankhyaField("12345678000100"));
        SankhyaSaveResponse.Entities entities = new SankhyaSaveResponse.Entities("1", entity);
        SankhyaSaveResponse saveResponse = new SankhyaSaveResponse(
                "saveRecord", "1", null, null, new SankhyaSaveResponse.ResponseBody(entities));

        when(clientRepository.existsByCgcCpf("12345678000100")).thenReturn(false);
        when(clientMapper.toEntity(request)).thenReturn(clientEntity);
        when(clientRepository.save(clientEntity)).thenReturn(clientEntity);
        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.saveRecord(anyString(), any())).thenReturn(saveResponse);
        when(clientMapper.toResponse(clientEntity)).thenReturn(clientResponse);

        ClientResponse result = clientService.create(request);

        assertThat(result.cgcCpf()).isEqualTo("12345678000100");
        verify(clientRepository).existsByCgcCpf("12345678000100");
        verify(sankhyaClient).saveRecord(anyString(), any());
        // deve atualizar codSankhya e salvar novamente
        verify(clientRepository, times(2)).save(clientEntity);
    }

    @Test
    @DisplayName("create: deve lançar DuplicateResourceException quando CNPJ/CPF já existe")
    void givenDuplicateCgcCpfWhenCreateShouldThrowDuplicateResourceException() {
        ClientRequest request = new ClientRequest("Emp X", "12345678000100",
                "Emp X", "Emp X Ltda", 1, PersonType.J, "C");

        when(clientRepository.existsByCgcCpf("12345678000100")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("12345678000100");

        verify(clientRepository, never()).save(any());
        verify(sankhyaClient, never()).saveRecord(anyString(), any());
    }

    @Test
    @DisplayName("create: deve fazer rollback local quando Sankhya recusar o cadastro após retry")
    void givenSankhyaRejectsAfterRetryWhenCreateShouldDeleteLocalAndThrowIntegrationException() {
        ClientRequest request = new ClientRequest("Emp X", "12345678000100",
                "Emp X", "Emp X Ltda", 1, PersonType.J, "C");

        Client clientEntity = Client.builder().id(1L).cgcCpf("12345678000100").nome("Emp X").build();

        SankhyaSaveResponse failResponse = new SankhyaSaveResponse(
                "saveRecord", "0", "Erro de negócio", null, null);

        when(clientRepository.existsByCgcCpf("12345678000100")).thenReturn(false);
        when(clientMapper.toEntity(request)).thenReturn(clientEntity);
        when(clientRepository.save(clientEntity)).thenReturn(clientEntity);
        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaAuthService.renewSessionCookie()).thenReturn("JSESSIONID=new");
        when(sankhyaClient.saveRecord(anyString(), any())).thenReturn(failResponse);

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Sankhya recusou");

        verify(clientRepository).delete(clientEntity);
    }

    @Test
    @DisplayName("create: deve renovar sessão e tentar novamente quando primeira tentativa falhar")
    void givenExpiredSessionWhenCreateShouldRenewAndRetry() {
        ClientRequest request = new ClientRequest("Emp X", "12345678000100",
                "Emp X", "Emp X Ltda", 1, PersonType.J, "C");

        Client clientEntity = Client.builder().id(1L).cgcCpf("12345678000100").nome("Emp X").build();
        ClientResponse clientResponse = new ClientResponse(1L, "Emp X", "12345678000100",
                "Emp X", "Emp X Ltda", 1, PersonType.J, "C", null, null, null);

        SankhyaSaveResponse failResponse = new SankhyaSaveResponse(
                "saveRecord", "0", "Sessão expirada", null, null);
        SankhyaSaveResponse.Entity entity = new SankhyaSaveResponse.Entity(
                new SankhyaField("5"), new SankhyaField("Emp X"), new SankhyaField("12345678000100"));
        SankhyaSaveResponse successResponse = new SankhyaSaveResponse(
                "saveRecord", "1", null, null,
                new SankhyaSaveResponse.ResponseBody(new SankhyaSaveResponse.Entities("1", entity)));

        when(clientRepository.existsByCgcCpf("12345678000100")).thenReturn(false);
        when(clientMapper.toEntity(request)).thenReturn(clientEntity);
        when(clientRepository.save(clientEntity)).thenReturn(clientEntity);
        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=expired");
        when(sankhyaAuthService.renewSessionCookie()).thenReturn("JSESSIONID=new");
        when(sankhyaClient.saveRecord(anyString(), any()))
                .thenReturn(failResponse)
                .thenReturn(successResponse);
        when(clientMapper.toResponse(clientEntity)).thenReturn(clientResponse);

        ClientResponse result = clientService.create(request);

        assertThat(result).isNotNull();
        verify(sankhyaAuthService).renewSessionCookie();
        verify(sankhyaClient, times(2)).saveRecord(anyString(), any());
    }

    @Test
    @DisplayName("create: deve fazer rollback local e lançar IntegrationException quando ocorrer erro de rede no Sankhya")
    void givenNetworkErrorWhenCreateShouldDeleteLocalAndThrowIntegrationException() {
        ClientRequest request = new ClientRequest("Emp X", "12345678000100",
                "Emp X", "Emp X Ltda", 1, PersonType.J, "C");
        Client clientEntity = Client.builder().id(1L).cgcCpf("12345678000100").nome("Emp X").build();

        when(clientRepository.existsByCgcCpf("12345678000100")).thenReturn(false);
        when(clientMapper.toEntity(request)).thenReturn(clientEntity);
        when(clientRepository.save(clientEntity)).thenReturn(clientEntity);
        when(sankhyaAuthService.getSessionCookie()).thenReturn("JSESSIONID=abc");
        when(sankhyaClient.saveRecord(anyString(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Falha na comunicação com o Sankhya");

        verify(clientRepository).delete(clientEntity);
    }
}






