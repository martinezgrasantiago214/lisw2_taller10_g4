package co.unicauca.saberpro.user.web;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.security.AuthenticatedUser;
import co.unicauca.saberpro.user.domain.User;

/** DTOs del API del user-service. Ninguna respuesta incluye contrasenas. */
public final class UserDtos {

    private UserDtos() {
    }

    public record RegisterUserRequest(String login, String fullName, String email, Role role, String password) {
    }

    /** El id es el identificador del usuario en Keycloak (UUID). */
    public record UserResponse(String id, String login, String fullName, String email, Role role, String status) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getLogin(), user.getFullName(), user.getEmail(),
                    user.getRole(), user.getStatus().name());
        }

        /** El usuario de la sesion, tal como lo describe su token (sin consultar a Keycloak). */
        public static UserResponse from(AuthenticatedUser user, String subject) {
            return new UserResponse(subject, user.login(), user.fullName(), user.email(), user.primaryRole(), "ACTIVO");
        }
    }
}
