package co.unicauca.saberpro.common.security;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

/**
 * Piezas de Spring para el flujo client credentials:
 * <ul>
 *   <li>{@link #fetcher}: pide el token al endpoint de Keycloak
 *       ({@code /realms/saberpro/protocol/openid-connect/token}).</li>
 *   <li>{@link #bearerInterceptor}: agrega {@code Authorization: Bearer ...}
 *       a cada peticion de un RestClient hacia otro microservicio; si la
 *       respuesta es 401, olvida el token para pedir uno nuevo la proxima vez.</li>
 * </ul>
 */
public final class KeycloakClientCredentials {

    private KeycloakClientCredentials() {
    }

    public static ServiceTokenProvider.TokenFetcher fetcher(String tokenUri, String clientId, String clientSecret) {
        RestClient client = RestClient.create();
        return () -> {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            Map<?, ?> body = client.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                return null;
            }
            Object token = body.get("access_token");
            Object expiresIn = body.get("expires_in");
            long seconds = expiresIn instanceof Number n ? n.longValue() : 60;
            return new ServiceTokenProvider.IssuedToken(token != null ? token.toString() : null, seconds);
        };
    }

    public static ServiceTokenProvider provider(String tokenUri, String clientId, String clientSecret) {
        return new ServiceTokenProvider(fetcher(tokenUri, clientId, clientSecret));
    }

    public static ClientHttpRequestInterceptor bearerInterceptor(ServiceTokenProvider tokens) {
        return new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                    throws IOException {
                request.getHeaders().setBearerAuth(tokens.accessToken());
                ClientHttpResponse response = execution.execute(request, body);
                if (response.getStatusCode().value() == 401) {
                    tokens.invalidate();
                }
                return response;
            }
        };
    }
}
