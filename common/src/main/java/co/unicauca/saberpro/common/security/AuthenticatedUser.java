package co.unicauca.saberpro.common.security;

import co.unicauca.saberpro.common.contract.Role;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Usuario que hizo la peticion, tal como lo certifica el token JWT emitido
 * por Keycloak. Los servicios toman de aqui el login del autor, del revisor o
 * del administrador; ya no confian en el login que venga en el cuerpo JSON.
 *
 * Es Java puro (sin Spring) para que los casos de uso y las pruebas no
 * dependan del framework de seguridad.
 */
public record AuthenticatedUser(String login, String fullName, String email, Set<Role> roles) {

    public AuthenticatedUser {
        Objects.requireNonNull(login, "login");
        roles = roles == null || roles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(roles));
        fullName = fullName == null || fullName.isBlank() ? login : fullName;
    }

    public static AuthenticatedUser of(String login, Role... roles) {
        return new AuthenticatedUser(login, login, null, roles.length == 0 ? Set.of() : EnumSet.of(roles[0], roles));
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean isAdministrator() {
        return hasRole(Role.ADMINISTRADOR);
    }

    /**
     * Rol "principal" para armar el menu del frontend. Si un usuario tuviera
     * varios roles, se elige el de mas privilegios.
     */
    public Role primaryRole() {
        if (hasRole(Role.ADMINISTRADOR)) {
            return Role.ADMINISTRADOR;
        }
        if (hasRole(Role.REVISOR)) {
            return Role.REVISOR;
        }
        if (hasRole(Role.AUTOR_PREGUNTAS)) {
            return Role.AUTOR_PREGUNTAS;
        }
        return null;
    }
}
