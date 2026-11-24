package co.unicauca.saberpro.frontend;

import co.unicauca.saberpro.frontend.client.ApiClient;
import co.unicauca.saberpro.frontend.client.AuthSession;
import co.unicauca.saberpro.frontend.client.KeycloakAuthClient;
import co.unicauca.saberpro.frontend.client.RestApis.RestQuestionApi;
import co.unicauca.saberpro.frontend.client.RestApis.RestReviewApi;
import co.unicauca.saberpro.frontend.client.RestApis.RestUserApi;
import co.unicauca.saberpro.frontend.facade.BancoPreguntasFacade;

/**
 * Contenedor de dependencias del frontend: se construye una vez al arrancar
 * y se comparte entre todas las ventanas. Las ventanas solo usan la fachada.
 *
 * La URL del API Gateway se puede cambiar con -Dsaberpro.gateway.url=...
 * y la de Keycloak con -Dsaberpro.keycloak.url=... (tercer corte).
 */
public final class AppContext {

    public static final String DEFAULT_GATEWAY_URL = "http://localhost:8080";
    public static final String DEFAULT_KEYCLOAK_URL = "http://localhost:8180";
    public static final String REALM = "saberpro";
    public static final String CLIENT_ID = "saberpro-frontend";

    private final BancoPreguntasFacade facade;

    public AppContext() {
        this(System.getProperty("saberpro.gateway.url", DEFAULT_GATEWAY_URL),
                System.getProperty("saberpro.keycloak.url", DEFAULT_KEYCLOAK_URL));
    }

    public AppContext(String gatewayUrl, String keycloakUrl) {
        AuthSession session = new AuthSession(new KeycloakAuthClient(keycloakUrl, REALM, CLIENT_ID));
        ApiClient client = new ApiClient(gatewayUrl, session);
        this.facade = new BancoPreguntasFacade(new RestUserApi(client, session), new RestQuestionApi(client),
                new RestReviewApi(client));
    }

    public AppContext(BancoPreguntasFacade facade) {
        this.facade = facade;
    }

    public BancoPreguntasFacade getFacade() {
        return facade;
    }
}
