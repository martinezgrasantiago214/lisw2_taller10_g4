package co.unicauca.saberpro.user.domain;

import co.unicauca.saberpro.common.contract.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la entidad User (vista del usuario de Keycloak). */
class UserTest {

    @Test
    void elConstructorAsignaTodosLosDatos() {
        User u = new User("kc-1", "autor1", "Autor Uno", "autor1@unicauca.edu.co", Role.AUTOR_PREGUNTAS,
                UserStatus.ACTIVO);

        assertEquals("kc-1", u.getId());
        assertEquals("autor1", u.getLogin());
        assertEquals("Autor Uno", u.getFullName());
        assertEquals("autor1@unicauca.edu.co", u.getEmail());
        assertEquals(Role.AUTOR_PREGUNTAS, u.getRole());
        assertTrue(u.hasRole(Role.AUTOR_PREGUNTAS));
        assertFalse(u.hasRole(Role.REVISOR));
        assertTrue(u.isActive());
    }

    @Test
    void unUsuarioNuevoNoTieneIdYQuedaActivo() {
        User u = User.newUser("rev1", "Revisor Uno", "r@x.co", Role.REVISOR);
        assertNull(u.getId());
        assertEquals(UserStatus.ACTIVO, u.getStatus());
    }

    @Test
    void unUsuarioInactivoNoEstaHabilitado() {
        assertFalse(new User("1", "x", "X Y", "x@y.co", Role.REVISOR, UserStatus.INACTIVO).isActive());
    }

    @Test
    void separaNombreYApellidosParaKeycloak() {
        User u = User.newUser("autor1", "  Maria  Fernanda Lopez Ruiz ", "m@x.co", Role.AUTOR_PREGUNTAS);
        assertEquals("Maria", u.firstName());
        assertEquals("Fernanda Lopez Ruiz", u.lastName());

        User solo = User.newUser("autor2", "Prince", "p@x.co", Role.AUTOR_PREGUNTAS);
        assertEquals("Prince", solo.firstName());
        assertEquals("", solo.lastName());
    }

    @Test
    void toStringMuestraNombreLoginYRol() {
        String texto = User.newUser("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS).toString();
        assertTrue(texto.contains("Autor Uno"));
        assertTrue(texto.contains("autor1"));
        assertTrue(texto.contains(Role.AUTOR_PREGUNTAS.getLabel()));
    }

    @Test
    void losEstadosDeUsuarioTienenEtiqueta() {
        assertEquals("Activo", UserStatus.ACTIVO.toString());
        assertEquals("Inactivo", UserStatus.INACTIVO.getLabel());
    }
}
