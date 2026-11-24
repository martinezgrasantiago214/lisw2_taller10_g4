package co.unicauca.saberpro.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Token de acceso de un microservicio para llamar a otro (comunicacion
 * servicio a servicio). Usa el flujo OAuth2 "client credentials" de Keycloak
 * con el cliente confidencial saberpro-backend: el servicio se autentica con
 * su propio secreto, no con el usuario.
 *
 * Guarda el token en memoria y solo pide uno nuevo cuando le faltan menos de
 * {@link #RENEW_BEFORE} para vencer, asi no se llama a Keycloak en cada
 * peticion. Java puro: la llamada HTTP la hace el {@link TokenFetcher}.
 */
public class ServiceTokenProvider {

    /** Margen para renovar el token antes de que venza. */
    static final Duration RENEW_BEFORE = Duration.ofSeconds(30);

    /** Respuesta del endpoint de tokens: el token y sus segundos de vigencia. */
    public record IssuedToken(String accessToken, long expiresInSeconds) {
    }

    /** Pide un token nuevo a Keycloak. */
    @FunctionalInterface
    public interface TokenFetcher {
        IssuedToken fetch();
    }

    private final TokenFetcher fetcher;
    private final Clock clock;
    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(TokenFetcher fetcher) {
        this(fetcher, Clock.systemUTC());
    }

    public ServiceTokenProvider(TokenFetcher fetcher, Clock clock) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** @return un token vigente; lo renueva si no hay o esta por vencer. */
    public synchronized String accessToken() {
        Instant now = clock.instant();
        if (token == null || !now.isBefore(expiresAt.minus(RENEW_BEFORE))) {
            IssuedToken issued = fetcher.fetch();
            if (issued == null || issued.accessToken() == null || issued.accessToken().isBlank()) {
                throw new IllegalStateException("Keycloak no devolvio un token de acceso para el servicio.");
            }
            token = issued.accessToken();
            expiresAt = now.plusSeconds(Math.max(issued.expiresInSeconds(), 0));
        }
        return token;
    }

    /** Olvida el token (p. ej. si otro servicio respondio 401) para pedir uno nuevo en la siguiente llamada. */
    public synchronized void invalidate() {
        token = null;
        expiresAt = Instant.EPOCH;
    }
}
