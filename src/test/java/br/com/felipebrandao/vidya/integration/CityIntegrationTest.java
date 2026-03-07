package br.com.felipebrandao.vidya.integration;

import br.com.felipebrandao.vidya.dto.request.LoginRequest;
import br.com.felipebrandao.vidya.repository.CityBatchRepository;
import br.com.felipebrandao.vidya.repository.CityRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CityIntegrationTest {

    private static final String SANKHYA_SERVICE_PATH = "/mge/service.sbr";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("sankhya.base-url", wireMock::baseUrl);
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private CityBatchRepository cityBatchRepository;

    @MockitoSpyBean
    private CityRepository cityRepository;

    private String token;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();

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
    @DisplayName("Deve retornar 200 com as cidades do banco após sincronização com o Sankhya")
    void givenValidTokenWhenListCitiesShouldReturn200WithCities() {
        // given
        stubSankhyaLoadCities("""
                {
                  "serviceName": "CRUDServiceProvider.loadRecords",
                  "status": "1",
                  "responseBody": {
                    "entities": {
                      "total": "2",
                      "hasMoreResult": "false",
                      "offsetPage": "0",
                      "entity": [
                        { "f0": { "$": "1234" }, "f1": { "$": "São Paulo" }, "f3": { "$": "SP" } },
                        { "f0": { "$": "5678" }, "f1": { "$": "Recife" },    "f3": { "$": "PE" } }
                      ]
                    }
                  }
                }
                """);

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/cities")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].codcid", equalTo(1234))
                .body("[0].nome", equalTo("São Paulo"))
                .body("[0].uf", equalTo("SP"));

        verify(cityRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar 200 com apenas o seed quando Sankhya retorna lista vazia")
    void givenValidTokenWhenSankhyaReturnsEmptyListShouldReturn200WithSeedOnly() {
        // given
        stubSankhyaLoadCities("""
                {
                  "serviceName": "CRUDServiceProvider.loadRecords",
                  "status": "1",
                  "responseBody": {
                    "entities": {
                      "total": "0",
                      "hasMoreResult": "false",
                      "offsetPage": "0",
                      "entity": null
                    }
                  }
                }
                """);

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/cities")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        verify(cityRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar 403 ao chamar /cities sem token")
    void givenNoTokenWhenListCitiesShouldReturn403() {
        given()
                .when()
                .get("/cities")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Deve percorrer múltiplas páginas do Sankhya ao sincronizar cidades")
    void givenSankhyaReturnsTwoPagesWhenListShouldCallLoadRecordsTwice() {
        // given
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.loadRecords"))
                .withRequestBody(WireMock.matchingJsonPath("$.requestBody.dataSet.offsetPage", WireMock.equalTo("0")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBodyFile("sankhya-load-cities-page0.json")));

        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.loadRecords"))
                .withRequestBody(WireMock.matchingJsonPath("$.requestBody.dataSet.offsetPage", WireMock.equalTo("1")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBodyFile("sankhya-load-cities-page1.json")));

        // when & then
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/cities")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        verify(cityRepository, times(1)).findAll();
    }

    private void stubSankhyaLogin() {
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("MobileLoginSP.login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Set-Cookie", "JSESSIONID=fake-session-id; Path=/")
                        .withHeader("Connection", "close")
                        .withBody("{\"serviceName\":\"MobileLoginSP.login\",\"status\":\"1\"}")));
    }

    private void stubSankhyaLoadCities(String responseBody) {
        wireMock.stubFor(post(urlPathEqualTo(SANKHYA_SERVICE_PATH))
                .withQueryParam("serviceName", WireMock.equalTo("CRUDServiceProvider.loadRecords"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBody(responseBody)));
    }
}
