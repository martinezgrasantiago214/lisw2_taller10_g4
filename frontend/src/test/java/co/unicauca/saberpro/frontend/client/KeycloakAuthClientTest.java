package co.unicauca.saberpro.frontend.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Login contra un servidor local que imita el endpoint de tokens de Keycloak
 * (realm saberpro): tokens, renovacion con refresh token y errores.
 */
class KeycloakAuthClientTest {

    private static final String TOKEN_PATH = "/realms/saberpro/protocol/openid-connect/token";

    /** Reloj que se puede adelantar. */
    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-23T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private HttpServer server;
    private String keycloakUrl;
    private final AtomicInteger issued = new AtomicInteger();
    private final AtomicInteger logouts = new AtomicInteger();
    private final MutableClock clock = new MutableClock();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(TOKEN_PATH, exchange -> {
            String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!form.contains("client_id=saberpro-frontend")) {
                respond(exchange, 401, "{\"error\":\"invalid_client\"}");
            } else if (form.contains("grant_type=password") && form.contains("password=Autor123%21")) {
                respond(exchange, 200, tokens());
            } else if (form.contains("grant_type=refresh_token") && form.contains("refresh_token=refresh-")) {
                respond(exchange, 200, tokens());
            } else {
                respond(exchange, 401, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}");
            }
        });
        server.createContext("/realms/saberpro/protocol/openid-connect/logout", exchange -> {
            logouts.incrementAndGet();
            respond(exchange, 204, "");
        });
        server.start();
        keycloakUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private String tokens() {
        int n = issued.incrementAndGet();
        return "{\"access_token\":\"access-" + n + "\",\"refresh_token\":\"refresh-" + n
                + "\",\"expires_in\":300,\"refresh_expires_in\":1800,\"token_type\":\"Bearer\"}";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    private KeycloakAuthClient client() {
        return new KeycloakAuthClient(keycloakUrl + "/", "saberpro", "saberpro-frontend", clock);
    }

    @Test
    void conCredencialesValidasDevuelveLosTokensYSuVencimiento() {
        KeycloakAuthClient.Tokens tokens = client().login("autor1", "Autor123!");

        assertEquals("access-1", tokens.accessToken());
        assertEquals("refresh-1", tokens.refreshToken());
        assertEquals(clock.now.plusSeconds(300), tokens.accessExpiresAt());
    }

    @Test
    void conClaveIncorrectaExplicaElErrorAlUsuario() {
        ApiException ex = assertThrows(ApiException.class, () -> client().login("autor1", "mala"));

        assertEquals(401, ex.getStatus());
        assertTrue(ex.getMessage().contains("Usuario o contrasena incorrectos"));
    }

    @Test
    void sinKeycloakSeInformaUnErrorDeConexion() {
        server.stop(0);
        ApiException ex = assertThrows(ApiException.class, () -> client().login("autor1", "Autor123!"));
        assertEquals(0, ex.getStatus());
        assertTrue(ex.getMessage().contains("Keycloak"));
    }

    @Test
    void laSesionReutilizaElTokenYLoRenuevaAntesDeVencer() {
        AuthSession session = new AuthSession(client(), clock);
        assertNull(session.get(), "sin sesion no hay token");

        session.login("autor1", "Autor123!");
        assertTrue(session.isAuthenticated());
        assertEquals("access-1", session.get());

        clock.now = clock.now.plusSeconds(200);
        assertEquals("access-1", session.get());

        clock.now = clock.now.plusSeconds(80);
        assertEquals("access-2", session.get(), "se renovo con el refresh token");
    }

    @Test
    void cerrarSesionAvisaAKeycloakYOlvidaLosTokens() {
        AuthSession session = new AuthSession(client(), clock);
        session.login("autor1", "Autor123!");

        session.logout();

        assertFalse(session.isAuthenticated());
        assertNull(session.get());
        assertEquals(1, logouts.get());
    }
}
