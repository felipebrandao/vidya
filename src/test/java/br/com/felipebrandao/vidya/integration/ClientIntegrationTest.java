package br.com.felipebrandao.vidya.integration;

import br.com.felipebrandao.vidya.dto.request.ClientRequest;
import br.com.felipebrandao.vidya.dto.request.LoginRequest;
import br.com.felipebrandao.vidya.entity.PersonType;
import br.com.felipebrandao.vidya.repository.ClientRepository;
import br.com.felipebrandao.vidya.service.SankhyaAuthService;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ClientIntegrationTest {

    private static final String SANKHYA_SERVICE_PATH = "/mge/service.sbr";
    private static final String VALID_CNPJ           = "07893870000186";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("sankhya.base-url", wireMock::baseUrl);
        registry.add("receitaws.url", wireMock::baseUrl);
    }

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private ClientRepository clientRepository;

    @Autowired
    private SankhyaAuthService sankhyaAuthService;

    private String token;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        sankhyaAuthService.resetSession();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        stubSankhyaLogin();

        token = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("user-seed", "password123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @Test
    @DisplayName("Deve retornar 200 com clientes sincronizados do Sankhya")
    void givenValidTokenWhenListClientsShouldReturn200WithClients() {
        // given
        stubSankhyaLoadClients("sankhya-load-clients.json");

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/clients")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].cgcCpf", equalTo("12.345.678/0001-99"))
                .body("[0].nomeparc", equalTo("Empresa Teste"));

        verify(clientRepository, times(1)).findAllByOrderByNomeAsc();
    }

    @Test
    @DisplayName("Deve retornar 403 ao listar clientes sem token")
    void givenNoTokenWhenListClientsShouldReturn403() {
        given()
                .when()
                .get("/clients")
                .then()
                .statusCode(403);
    }


    @Test
    @DisplayName("Deve retornar 200 com dados da empresa ao consultar CNPJ válido")
    void givenValidCnpjWhenLookupShouldReturn200WithCompanyData() {
        // given
        wireMock.stubFor(get(urlPathEqualTo("/cnpj/" + VALID_CNPJ))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("receitaws-cnpj-valid.json")));

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/clients/cnpj/{cnpj}", VALID_CNPJ)
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("cnpj", equalTo(VALID_CNPJ))
                .body("nome", equalTo("EMPRESA TESTE LTDA"))
                .body("municipio", equalTo("RECIFE"))
                .body("uf", equalTo("PE"));
    }

    @Test
    @DisplayName("Deve retornar 502 quando CNPJ não existe na ReceitaWS")
    void givenInvalidCnpjWhenLookupShouldReturn502() {
        // given
        wireMock.stubFor(get(urlPathEqualTo("/cnpj/00000000000000"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("receitaws-cnpj-error.json")));

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/clients/cnpj/{cnpj}", "00000000000000")
                .then()
                .statusCode(502);
    }

    @Test
    @DisplayName("Deve retornar 403 ao consultar CNPJ sem token")
    void givenNoTokenWhenLookupCnpjShouldReturn403() {
        given()
                .when()
                .get("/clients/cnpj/{cnpj}", VALID_CNPJ)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Deve criar cliente com sucesso e retornar 201 com CODPARC do Sankhya")
    void givenValidClientRequestWhenCreateShouldReturn201() {
        // given
        stubSankhyaSaveClient("sankhya-save-client-success.json");
        ClientRequest request = buildClientRequest("99.888.777/0001-66");

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/clients")
                .then()
                .statusCode(201)
                .body("cgcCpf", equalTo("99.888.777/0001-66"))
                .body("nomeparc", equalTo("Novo Cliente Ltda"))
                .body("codSankhya", equalTo(2001));

        verify(clientRepository, times(1)).existsByCgcCpf("99.888.777/0001-66");
        verify(clientRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Deve retornar 409 ao tentar criar cliente com CNPJ já cadastrado")
    void givenExistingCgcCpfWhenCreateShouldReturn409() {
        // given
        ClientRequest request = buildClientRequest("12.345.678/0001-99");

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/clients")
                .then()
                .statusCode(409);

        verify(clientRepository, times(1)).existsByCgcCpf("12.345.678/0001-99");
    }

    @Test
    @DisplayName("Deve retornar 403 ao criar cliente sem token")
    void givenNoTokenWhenCreateClientShouldReturn403() {
        given()
                .contentType(ContentType.JSON)
                .body(buildClientRequest("11.222.333/0001-44"))
                .when()
                .post("/clients")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Deve retornar 502 e deletar o cliente local quando Sankhya recusa o cadastro")
    void givenSankhyaRefusesWhenCreateShouldReturn502AndDeleteLocal() {
        // given
        stubSankhyaSaveClient("sankhya-save-client-error.json");
        ClientRequest request = buildClientRequest("55.444.333/0001-22");

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/clients")
                .then()
                .statusCode(502);

        verify(clientRepository, times(1)).existsByCgcCpf("55.444.333/0001-22");
        verify(clientRepository, times(1)).save(any());
        verify(clientRepository, times(1)).delete(any());
    }

    @Test
    @DisplayName("Deve retornar 200 com apenas o seed quando Sankhya retorna entity null no sync")
    void givenSankhyaReturnsNullEntityWhenListShouldReturn200WithSeedOnly() {
        // given
        stubSankhyaLoadClients("sankhya-load-clients-empty.json");

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/clients")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        verify(clientRepository, times(1)).findAllByOrderByNomeAsc();
    }

    @Test
    @DisplayName("Deve inserir novo cliente via syncClients quando codSankhya não existe no banco")
    void givenNewClientFromSankhyaWhenListShouldSaveViaBuilder() {
        // given
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.loadRecords"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("sankhya-load-client-new.json")));

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/clients")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].cgcCpf", equalTo("77.666.555/0001-88"))
                .body("[0].nomeparc", equalTo("Cliente Novo Via Sync"))
                .body("[0].codSankhya", equalTo(9999));

        verify(clientRepository, times(1)).save(any());
        verify(clientRepository, times(1)).findAllByOrderByNomeAsc();
    }

    @Test
    @DisplayName("Deve percorrer múltiplas páginas do Sankhya e retornar todos os clientes")
    void givenSankhyaReturnsTwoPagesWhenListShouldReturnAllClients() {
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.loadRecords"))
                .withRequestBody(matchingJsonPath("$.requestBody.dataSet.offsetPage", WireMock.equalTo("0")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBodyFile("sankhya-load-clients-page0.json")));

        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.loadRecords"))
                .withRequestBody(matchingJsonPath("$.requestBody.dataSet.offsetPage", WireMock.equalTo("1")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBodyFile("sankhya-load-clients-page1.json")));

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/clients")
                .then()
                .statusCode(200)
                .body("$", hasSize(3));

        verify(clientRepository, times(2)).findByCodSankhya(any());
        verify(clientRepository, times(1)).findAllByOrderByNomeAsc();
    }

    private void stubSankhyaLogin() {
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("MobileLoginSP.login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Set-Cookie", "JSESSIONID=fake-session-id; Path=/")
                        .withHeader("Connection", "close")
                        .withBodyFile("sankhya-login.json")));
    }

    private void stubSankhyaLoadClients(String bodyFile) {
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.loadRecords"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBodyFile(bodyFile)));
    }

    private void stubSankhyaSaveClient(String bodyFile) {
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.saveRecord"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBodyFile(bodyFile)));
    }

    private ClientRequest buildClientRequest(String cgcCpf) {
        return new ClientRequest(
                "Novo Cliente Ltda",
                cgcCpf,
                "Novo Cliente Ltda",
                "Novo Cliente Razão Social",
                1234,
                PersonType.J,
                "A1"
        );
    }
}
