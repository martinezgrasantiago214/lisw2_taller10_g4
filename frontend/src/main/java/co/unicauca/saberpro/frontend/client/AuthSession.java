package co.unicauca.saberpro.frontend.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sesion del usuario en el frontend: guarda los tokens de Keycloak en
 * memoria (nunca en disco) y entrega un token de acceso vigente para cada
 * peticion al API Gateway. Si al token le falta poco para vencer, lo renueva
 * con el refresh token sin pedirle la clave otra vez al usuario.
 */
public class AuthSession implements Supplier<String> {

    /** Margen para renovar el token antes de que venza. */
    static final Duration RENEW_BEFORE = Duration.ofSeconds(30);

    private final KeycloakAuthClient auth;
    private final Clock clock;
    private KeycloakAuthClient.Tokens tokens;

    public AuthSession(KeycloakAuthClient auth) {
        this(auth, Clock.systemUTC());
    }

    public AuthSession(KeycloakAuthClient auth, Clock clock) {
        this.auth = auth;
        this.clock = clock;
    }

    public synchronized void login(String username, String password) {
        tokens = auth.login(username, password);
    }

    public synchronized boolean isAuthenticated() {
        return tokens != null;
    }

    /**
     * Supplier del ApiClient: token vigente, o null si no hay sesion (las
     * peticiones publicas, como el registro, van sin token).
     */
    @Override
    public synchronized String get() {
        if (tokens == null) {
            return null;
        }
        Instant now = clock.instant();
        if (now.isBefore(tokens.accessExpiresAt().minus(RENEW_BEFORE))) {
            return tokens.accessToken();
        }
        if (tokens.refreshToken() == null) {
            tokens = null;
            throw new ApiException(401, List.of("Su sesion expiro. Inicie sesion de nuevo."));
        }
        try {
            tokens = auth.refresh(tokens.refreshToken());
        } catch (ApiException ex) {
            tokens = null;
            throw new ApiException(401, List.of("Su sesion expiro. Inicie sesion de nuevo."));
        }
        return tokens.accessToken();
    }

    public synchronized void logout() {
        if (tokens != null) {
            auth.logout(tokens.refreshToken());
        }
        tokens = null;
    }
}
