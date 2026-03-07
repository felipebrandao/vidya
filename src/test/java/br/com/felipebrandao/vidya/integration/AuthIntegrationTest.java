package br.com.felipebrandao.vidya.integration;

import br.com.felipebrandao.vidya.dto.request.LoginRequest;
import br.com.felipebrandao.vidya.dto.request.RegisterRequest;
import br.com.felipebrandao.vidya.entity.User;
import br.com.felipebrandao.vidya.repository.UserRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso e retornar token")
    void givenValidRegisterRequestWhenRegisterShouldReturnCreated() {
        RegisterRequest request = new RegisterRequest("testuser", "password123");

        given()
            .contentType("application/json")
            .body(request)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(201)
            .body("token", notNullValue());

        verify(userRepository, times(1)).existsByUsername(eq("testuser"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve autenticar o usuário seed e retornar token")
    void givenSeededUserWhenLoginShouldReturnToken() {
        LoginRequest request = new LoginRequest("user-seed", "password123");

        given()
            .contentType("application/json")
            .body(request)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());

        verify(userRepository, times(2)).findByUsername(eq("user-seed"));
    }

    @Test
    @DisplayName("Deve retornar erro ao tentar registrar com username já em uso")
    void givenExistingUsernameWhenRegisterShouldReturnConflict() {
        RegisterRequest request = new RegisterRequest("user-seed", "password123");

        given()
            .contentType("application/json")
            .body(request)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(409);

        verify(userRepository, times(1)).existsByUsername(eq("user-seed"));
    }
}
