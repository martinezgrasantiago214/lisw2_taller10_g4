package co.unicauca.saberpro.common.security;

import co.unicauca.saberpro.common.contract.Role;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lee los "claims" de un token de Keycloak. Java puro: recibe el mapa de
 * claims, asi se prueba sin crear tokens firmados.
 *
 * Keycloak pone los roles del realm en {@code realm_access.roles} y el login
 * en {@code preferred_username}.
 */
public final class KeycloakClaims {

    public static final String USERNAME = "preferred_username";
    public static final String REALM_ACCESS = "realm_access";
    public static final String ROLES = "roles";

    private KeycloakClaims() {
    }

    /** Todos los roles del realm que trae el token, incluidos los tecnicos (p. ej. SERVICIO). */
    public static Set<String> realmRoles(Map<String, Object> claims) {
        Set<String> result = new LinkedHashSet<>();
        Object realmAccess = claims == null ? null : claims.get(REALM_ACCESS);
        if (realmAccess instanceof Map<?, ?> access && access.get(ROLES) instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role != null) {
                    result.add(role.toString());
                }
            }
        }
        return result;
    }

    /** Roles de negocio ({@link Role}); los roles que el sistema no conoce se ignoran. */
    public static Set<Role> businessRoles(Map<String, Object> claims) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (String name : realmRoles(claims)) {
            for (Role role : Role.values()) {
                if (role.name().equalsIgnoreCase(name)) {
                    roles.add(role);
                }
            }
        }
        return roles;
    }

    /** Login del usuario: preferred_username o, si no viene, el "sub" del token. */
    public static String username(Map<String, Object> claims) {
        Object username = claims == null ? null : claims.get(USERNAME);
        if (username != null && !username.toString().isBlank()) {
            return username.toString();
        }
        Object subject = claims == null ? null : claims.get("sub");
        return subject != null ? subject.toString() : null;
    }

    public static AuthenticatedUser toUser(Map<String, Object> claims) {
        String fullName = text(claims, "name");
        if (fullName == null) {
            String given = text(claims, "given_name");
            String family = text(claims, "family_name");
            fullName = given == null ? family : family == null ? given : given + " " + family;
        }
        return new AuthenticatedUser(username(claims), fullName, text(claims, "email"), businessRoles(claims));
    }

    private static String text(Map<String, Object> claims, String key) {
        Object value = claims == null ? null : claims.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
