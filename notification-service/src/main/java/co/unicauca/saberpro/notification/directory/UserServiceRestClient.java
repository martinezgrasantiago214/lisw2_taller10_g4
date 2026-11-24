package co.unicauca.saberpro.notification.directory;

import co.unicauca.saberpro.notification.domain.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * Implementacion de {@link UserDirectory} que consulta el API REST del
 * user-service (GET /api/users?role=ADMINISTRADOR y /api/users/{login}); el
 * notification-service nunca lee la base de datos de usuarios.
 *
 * Tercer corte: el user-service exige un token JWT. El RestClient que recibe
 * este adaptador ya trae el interceptor que agrega el token de servicio
 * (client credentials del cliente saberpro-backend en Keycloak).
 */
public class UserServiceRestClient implements UserDirectory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceRestClient.class);

    private final RestClient restClient;

    public UserServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<Recipient> findByLogin(String login) {
        try {
            return Optional.ofNullable(restClient.get().uri("/api/users/{login}", login)
                    .retrieve().body(Recipient.class));
        } catch (RestClientException ex) {
            LOGGER.warn("No fue posible consultar el usuario {} en el user-service: {}", login, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Recipient> findAdministrators() {
        try {
            List<Recipient> users = restClient.get().uri("/api/users?role=ADMINISTRADOR").retrieve()
                    .body(new ParameterizedTypeReference<List<Recipient>>() { });
            return users == null ? List.of()
                    : users.stream().filter(u -> "ADMINISTRADOR".equals(u.role())).toList();
        } catch (RestClientException ex) {
            LOGGER.warn("No fue posible consultar los administradores en el user-service: {}", ex.getMessage());
            return List.of();
        }
    }
}
