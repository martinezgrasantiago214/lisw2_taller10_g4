package co.unicauca.saberpro.frontend.client;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.frontend.client.RestApis.RestUserApi;
import co.unicauca.saberpro.frontend.model.Models.UserView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Cliente HTTP del frontend contra un servidor local que imita al API Gateway protegido con JWT. */
class ApiClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/users/me", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (!"Bearer tok-123".equals(auth)) {
                respond(exchange, 401, "{\"status\":401,\"messages\":[\"sin token\"]}");
                return;
            }
            respond(exchange, 200, "{\"id\":\"kc-1\",\"login\":\"autor1\",\"fullName\":\"Autor Uno\","
                    + "\"email\":\"a@x.co\",\"role\":\"AUTOR_PREGUNTAS\",\"status\":\"ACTIVO\",\"extra\":\"se ignora\"}");
        });
        server.createContext("/api/questions", exchange -> respond(exchange, 501,
                "{\"status\":501,\"error\":\"Not Implemented\",\"messages\":"
                        + "[\"Pendiente de implementar (PSP-18): crear y almacenar preguntas (HU01)\"]}"));
        server.createContext("/api/users", exchange -> respond(exchange, 502, "Bad Gateway"));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    void enviaElTokenJwtYConvierteLaRespuestaIgnorandoCamposDesconocidos() {
        ApiClient client = new ApiClient(baseUrl, () -> "tok-123");

        UserView user = client.get("/api/users/me", new TypeReference<UserView>() { });

        assertEquals("kc-1", user.id());
        assertEquals("autor1", user.login());
        assertEquals(Role.AUTOR_PREGUNTAS, user.role());
    }

    @Test
    void sinTokenElGatewayResponde401YSePideIniciarSesion() {
        ApiClient client = new ApiClient(baseUrl);

        ApiException ex = assertThrows(ApiException.class,
                () -> client.get("/api/users/me", new TypeReference<UserView>() { }));

        assertTrue(ex.isUnauthorized());
        assertTrue(ex.getMessage().contains("Inicie sesion"));
    }

    @Test
    void unCasoDeUsoPendienteLlegaComoFuncionalidadPendienteConSuIssue() {
        ApiException ex = assertThrows(ApiException.class,
                () -> new RestApis.RestQuestionApi(new ApiClient(baseUrl)).findById("q-1"));

        assertTrue(ex.isPendingFeature());
        assertTrue(ex.getMessage().contains("PSP-18"));
    }

    @Test
    void unErrorSinCuerpoJsonUsaElCodigoHttp() {
        ApiException ex = assertThrows(ApiException.class,
                () -> new RestUserApi(new ApiClient(baseUrl), null).listReviewers());

        assertEquals(502, ex.getStatus());
        assertFalse(ex.isPendingFeature());
    }

    @Test
    void siElGatewayNoRespondeSeInformaUnErrorDeConexion() {
        server.stop(0);
        ApiException ex = assertThrows(ApiException.class,
                () -> new RestUserApi(new ApiClient(baseUrl), null).listReviewers());

        assertEquals(0, ex.getStatus());
        assertTrue(ex.getMessage().contains("No fue posible conectar"));
    }
}
