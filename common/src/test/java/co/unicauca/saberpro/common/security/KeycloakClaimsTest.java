package co.unicauca.saberpro.common.security;

import co.unicauca.saberpro.common.contract.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Lectura de los claims de un token de Keycloak: login, nombre y roles del realm. */
class KeycloakClaimsTest {

    private static Map<String, Object> claims(String username, List<String> roles) {
        return Map.of(
                "sub", "8a1f-uuid",
                "preferred_username", username,
                "given_name", "Docente",
                "family_name", "Autor",
                "email", username + "@unicauca.edu.co",
                "realm_access", Map.of("roles", roles));
    }

    @Test
    void tomaElLoginDePreferredUsername() {
        assertEquals("autor1", KeycloakClaims.username(claims("autor1", List.of())));
    }

    @Test
    void siNoHayPreferredUsernameUsaElSubject() {
        assertEquals("abc", KeycloakClaims.username(Map.of("sub", "abc")));
    }

    @Test
    void leeTodosLosRolesDelRealmIncluidosLosTecnicos() {
        Set<String> roles = KeycloakClaims.realmRoles(claims("svc", List.of("SERVICIO", "offline_access")));
        assertEquals(Set.of("SERVICIO", "offline_access"), roles);
    }

    @Test
    void soloLosRolesConocidosSonRolesDeNegocio() {
        Set<Role> roles = KeycloakClaims.businessRoles(
                claims("rev1", List.of("REVISOR", "default-roles-saberpro", "uma_authorization")));
        assertEquals(Set.of(Role.REVISOR), roles);
    }

    @Test
    void sinRealmAccessNoHayRoles() {
        assertTrue(KeycloakClaims.realmRoles(Map.of("sub", "x")).isEmpty());
        assertTrue(KeycloakClaims.realmRoles(null).isEmpty());
    }

    @Test
    void armaElUsuarioAutenticadoConNombreCompletoYCorreo() {
        AuthenticatedUser user = KeycloakClaims.toUser(claims("autor1", List.of("AUTOR_PREGUNTAS")));

        assertEquals("autor1", user.login());
        assertEquals("Docente Autor", user.fullName());
        assertEquals("autor1@unicauca.edu.co", user.email());
        assertTrue(user.hasRole(Role.AUTOR_PREGUNTAS));
        assertFalse(user.isAdministrator());
        assertEquals(Role.AUTOR_PREGUNTAS, user.primaryRole());
    }

    @Test
    void elRolPrincipalEsElDeMasPrivilegios() {
        AuthenticatedUser user = AuthenticatedUser.of("admin", Role.REVISOR, Role.ADMINISTRADOR);
        assertEquals(Role.ADMINISTRADOR, user.primaryRole());
        assertNull(AuthenticatedUser.of("nadie").primaryRole());
    }

    @Test
    void sinNombreElNombreCompletoEsElLogin() {
        assertEquals("rev1", new AuthenticatedUser("rev1", " ", null, null).fullName());
    }
}
