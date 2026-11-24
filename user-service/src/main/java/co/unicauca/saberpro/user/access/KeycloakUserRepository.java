package co.unicauca.saberpro.user.access;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.ConflictException;
import co.unicauca.saberpro.user.domain.User;
import co.unicauca.saberpro.user.domain.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador del puerto {@link IUserRepository} sobre el Admin REST API de
 * Keycloak ({@code /admin/realms/saberpro/...}). Keycloak es la unica fuente
 * de verdad de usuarios, credenciales y roles; el user-service ya no guarda
 * contrasenas ni tiene base de datos propia.
 *
 * El RestClient trae el token del cliente confidencial saberpro-backend
 * (client credentials); su cuenta de servicio tiene en Keycloak los roles
 * view-users, query-users, manage-users y view-realm de realm-management.
 *
 * Los roles de negocio son roles del realm con el mismo nombre que el enum
 * {@link Role}: ADMINISTRADOR, AUTOR_PREGUNTAS y REVISOR.
 */
public class KeycloakUserRepository implements IUserRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakUserRepository.class);
    private static final int MAX_USERS = 1000;

    /** UserRepresentation de Keycloak (solo los campos que se usan). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record KeycloakUser(String id, String username, String firstName, String lastName, String email,
                        Boolean enabled, Boolean emailVerified, List<Credential> credentials) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Credential(String type, String value, Boolean temporary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeycloakRole(String id, String name) {
    }

    private final RestClient admin;

    /** @param admin RestClient con baseUrl {@code <keycloak>/admin/realms/<realm>} y el token de servicio. */
    public KeycloakUserRepository(RestClient admin) {
        this.admin = admin;
    }

    @Override
    public User create(User newUser, String plainPassword) {
        KeycloakUser body = new KeycloakUser(null, newUser.getLogin(), newUser.firstName(), newUser.lastName(),
                newUser.getEmail(), true, true, List.of(new Credential("password", plainPassword, false)));
        String id;
        try {
            ResponseEntity<Void> response = admin.post().uri("/users").body(body).retrieve().toBodilessEntity();
            URI location = response.getHeaders().getLocation();
            id = location != null ? location.getPath().substring(location.getPath().lastIndexOf('/') + 1)
                    : find(newUser.getLogin()).map(KeycloakUser::id).orElseThrow();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ConflictException("Ya existe un usuario con el login " + newUser.getLogin() + " o con ese correo.");
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new BusinessException("Keycloak rechazo los datos del usuario: " + keycloakMessage(ex));
        } catch (RestClientException ex) {
            throw unavailable("crear el usuario " + newUser.getLogin(), ex);
        }

        try {
            KeycloakRole role = admin.get().uri("/roles/{name}", newUser.getRole().name()).retrieve()
                    .body(KeycloakRole.class);
            admin.post().uri("/users/{id}/role-mappings/realm", id).body(List.of(role)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            // El usuario quedo creado sin rol: se borra para no dejarlo a medias.
            try {
                admin.delete().uri("/users/{id}", id).retrieve().toBodilessEntity();
            } catch (RestClientException cleanup) {
                LOGGER.error("No fue posible borrar el usuario {} creado sin rol", newUser.getLogin(), cleanup);
            }
            throw unavailable("asignar el rol " + newUser.getRole() + " a " + newUser.getLogin(), ex);
        }
        return new User(id, newUser.getLogin(), newUser.getFullName(), newUser.getEmail(), newUser.getRole(),
                UserStatus.ACTIVO);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        Optional<KeycloakUser> found = find(login.trim());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        KeycloakUser user = found.get();
        try {
            List<KeycloakRole> roles = admin.get().uri("/users/{id}/role-mappings/realm", user.id()).retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakRole>>() { });
            return Optional.of(toUser(user, businessRole(roles)));
        } catch (RestClientException ex) {
            throw unavailable("consultar los roles de " + login, ex);
        }
    }

    @Override
    public List<User> listByRole(Role role) {
        try {
            List<KeycloakUser> users = admin.get()
                    .uri("/roles/{role}/users?first=0&max={max}", role.name(), MAX_USERS)
                    .retrieve().body(new ParameterizedTypeReference<List<KeycloakUser>>() { });
            return users == null ? List.of() : users.stream().map(u -> toUser(u, role)).toList();
        } catch (HttpClientErrorException.NotFound ex) {
            LOGGER.warn("El rol {} no existe en el realm de Keycloak", role);
            return List.of();
        } catch (RestClientException ex) {
            throw unavailable("listar los usuarios con rol " + role, ex);
        }
    }

    @Override
    public List<User> list() {
        // Un usuario aparece una sola vez, con el rol de mas privilegios (ADMINISTRADOR, REVISOR, AUTOR).
        Map<String, User> users = new LinkedHashMap<>();
        for (Role role : List.of(Role.ADMINISTRADOR, Role.REVISOR, Role.AUTOR_PREGUNTAS)) {
            for (User user : listByRole(role)) {
                users.putIfAbsent(user.getLogin(), user);
            }
        }
        return new ArrayList<>(users.values());
    }

    // ---------------------------------------------------------------- auxiliares

    private Optional<KeycloakUser> find(String login) {
        try {
            List<KeycloakUser> users = admin.get().uri("/users?username={login}&exact=true", login).retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakUser>>() { });
            return users == null ? Optional.empty()
                    : users.stream().filter(u -> login.equalsIgnoreCase(u.username())).findFirst();
        } catch (RestClientException ex) {
            throw unavailable("consultar el usuario " + login, ex);
        }
    }

    /** Rol de negocio con mas privilegios entre los roles del realm del usuario. */
    static Role businessRole(List<KeycloakRole> roles) {
        if (roles == null) {
            return null;
        }
        List<String> names = roles.stream().map(KeycloakRole::name).toList();
        for (Role role : List.of(Role.ADMINISTRADOR, Role.REVISOR, Role.AUTOR_PREGUNTAS)) {
            if (names.contains(role.name())) {
                return role;
            }
        }
        return null;
    }

    static User toUser(KeycloakUser u, Role role) {
        String fullName = ((u.firstName() == null ? "" : u.firstName()) + " "
                + (u.lastName() == null ? "" : u.lastName())).trim();
        return new User(u.id(), u.username(), fullName.isEmpty() ? u.username() : fullName, u.email(), role,
                Boolean.FALSE.equals(u.enabled()) ? UserStatus.INACTIVO : UserStatus.ACTIVO);
    }

    private static String keycloakMessage(HttpClientErrorException ex) {
        String body = ex.getResponseBodyAsString();
        return body == null || body.isBlank() ? ex.getStatusText() : body;
    }

    private static IdentityProviderUnavailableException unavailable(String action, RestClientException ex) {
        LOGGER.warn("Keycloak no respondio al {}: {}", action, ex.getMessage());
        return new IdentityProviderUnavailableException("No fue posible " + action
                + ": el servidor de identidad (Keycloak) no responde. Intente de nuevo.", ex);
    }
}
