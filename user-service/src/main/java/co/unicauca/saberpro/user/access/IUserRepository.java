package co.unicauca.saberpro.user.access;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto del directorio de usuarios (DIP). UserService depende de esta
 * abstraccion; en ejecucion la implementa {@link KeycloakUserRepository}
 * (Admin REST API de Keycloak) y en las pruebas un doble en memoria.
 */
public interface IUserRepository {

    /**
     * Crea el usuario con su contrasena y su rol.
     *
     * @throws co.unicauca.saberpro.common.web.ConflictException si ya existe el login o el correo.
     * @throws co.unicauca.saberpro.common.web.BusinessException si el proveedor rechaza los datos (p. ej. su politica de claves).
     */
    User create(User newUser, String plainPassword);

    Optional<User> findByLogin(String login);

    default boolean existsByLogin(String login) {
        return findByLogin(login).isPresent();
    }

    List<User> listByRole(Role role);

    /** Todos los usuarios con algun rol del sistema. */
    List<User> list();
}
