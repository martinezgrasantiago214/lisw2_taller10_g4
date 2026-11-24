package co.unicauca.saberpro.user.access;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.access.KeycloakUserRepository.KeycloakRole;
import co.unicauca.saberpro.user.access.KeycloakUserRepository.KeycloakUser;
import co.unicauca.saberpro.user.domain.User;
import co.unicauca.saberpro.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Traduccion de las representaciones de Keycloak a la entidad User. */
class KeycloakMappingTest {

    @Test
    void elRolDeNegocioEsElDeMasPrivilegios() {
        assertEquals(Role.ADMINISTRADOR, KeycloakUserRepository.businessRole(List.of(
                new KeycloakRole("1", "default-roles-saberpro"), new KeycloakRole("2", "REVISOR"),
                new KeycloakRole("3", "ADMINISTRADOR"))));
        assertEquals(Role.AUTOR_PREGUNTAS, KeycloakUserRepository.businessRole(List.of(
                new KeycloakRole("1", "AUTOR_PREGUNTAS"))));
        assertNull(KeycloakUserRepository.businessRole(List.of(new KeycloakRole("1", "offline_access"))));
        assertNull(KeycloakUserRepository.businessRole(null));
    }

    @Test
    void armaElNombreCompletoYElEstado() {
        User u = KeycloakUserRepository.toUser(new KeycloakUser("kc-1", "rev1", "Docente", "Revisor Uno",
                "rev1@x.co", false, true, null), Role.REVISOR);

        assertEquals("kc-1", u.getId());
        assertEquals("Docente Revisor Uno", u.getFullName());
        assertEquals(UserStatus.INACTIVO, u.getStatus());

        User sinNombre = KeycloakUserRepository.toUser(new KeycloakUser("kc-2", "svc", null, null, null, null,
                null, null), null);
        assertEquals("svc", sinNombre.getFullName());
        assertTrue(sinNombre.isActive());
    }
}
