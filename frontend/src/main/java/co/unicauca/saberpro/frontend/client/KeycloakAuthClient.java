package co.unicauca.saberpro.frontend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cliente del servidor de identidad Keycloak (tercer corte): obtiene, renueva
 * y revoca los tokens JWT del usuario.
 *
 * El frontend Swing es un cliente publico (saberpro-frontend, sin secreto)
 * que usa el flujo "Resource Owner Password Credentials": el usuario escribe
 * su clave en la ventana de login y se envia solo a Keycloak, nunca a los
 * microservicios. Los microservicios solo ven el token firmado.
 */
public class KeycloakAuthClient {

    /** Tokens de una sesion: el de acceso (corto) y el de renovacion (mas largo). */
    public record Tokens(String accessToken, String refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt) {
    }

    private final String tokenEndpoint;
    private final String logoutEndpoint;
    private final String clientId;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock;

    public KeycloakAuthClient(String keycloakUrl, String realm, String clientId) {
        this(keycloakUrl, realm, clientId, Clock.systemUTC());
    }

    public KeycloakAuthClient(String keycloakUrl, String realm, String clientId, Clock clock) {
        String base = keycloakUrl.endsWith("/") ? keycloakUrl.substring(0, keycloakUrl.length() - 1) : keycloakUrl;
        this.tokenEndpoint = base + "/realms/" + realm + "/protocol/openid-connect/token";
        this.logoutEndpoint = base + "/realms/" + realm + "/protocol/openid-connect/logout";
        this.clientId = clientId;
        this.clock = clock;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** Inicia sesion con usuario y clave. */
    public Tokens login(String username, String password) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", clientId);
        form.put("username", username == null ? "" : username);
        form.put("password", password == null ? "" : password);
        form.put("scope", "openid");
        return requestTokens(form);
    }

    /** Renueva el token de acceso con el de renovacion. */
    public Tokens refresh(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", clientId);
        form.put("refresh_token", refreshToken);
        return requestTokens(form);
    }

    /** Cierra la sesion en Keycloak (mejor esfuerzo: si falla, el token igual vence solo). */
    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        form.put("refresh_token", refreshToken);
        try {
            http.send(post(logoutEndpoint, form), HttpResponse.BodyHandlers.discarding());
        } catch (IOException ex) {
            // sin conexion: no hay nada que hacer
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private Tokens requestTokens(Map<String, String> form) {
        HttpResponse<String> response;
        try {
            response = http.send(post(tokenEndpoint, form), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new ApiException(0, List.of("No fue posible conectar con el servidor de identidad (Keycloak) en "
                    + tokenEndpoint + ". Verifique que este en ejecucion."));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, List.of("La peticion fue interrumpida."));
        }
        JsonNode body = parse(response.body());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), List.of(describeError(body)));
        }
        Instant now = clock.instant();
        return new Tokens(text(body, "access_token"), text(body, "refresh_token"),
                now.plusSeconds(body.path("expires_in").asLong(60)),
                now.plusSeconds(body.path("refresh_expires_in").asLong(0)));
    }

    /** Mensajes de Keycloak traducidos para el usuario. */
    static String describeError(JsonNode body) {
        String error = text(body, "error");
        String description = text(body, "error_description");
        if ("invalid_grant".equals(error) && description != null && description.toLowerCase().contains("not fully set up")) {
            return "La cuenta aun no esta lista en Keycloak (faltan datos del perfil o una accion requerida).";
        }
        if ("invalid_grant".equals(error) && description != null && description.toLowerCase().contains("session")) {
            return "Su sesion expiro. Inicie sesion de nuevo.";
        }
        if ("invalid_grant".equals(error)) {
            return "Usuario o contrasena incorrectos, o usuario deshabilitado.";
        }
        if ("unauthorized_client".equals(error) || "invalid_client".equals(error)) {
            return "El cliente saberpro-frontend no esta configurado en Keycloak.";
        }
        return "Keycloak rechazo la solicitud" + (description != null ? ": " + description : ".");
    }

    private HttpRequest post(String url, Map<String, String> form) {
        String encoded = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(encoded, StandardCharsets.UTF_8))
                .build();
    }

    private JsonNode parse(String body) {
        try {
            return body == null || body.isBlank() ? mapper.createObjectNode() : mapper.readTree(body);
        } catch (IOException ex) {
            return mapper.createObjectNode();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
