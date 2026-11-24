package co.unicauca.saberpro.user.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Politica de contrasenas que se valida antes de crear el usuario en Keycloak. */
class PasswordPolicyTest {

    private final IPasswordPolicy policy = new DefaultPasswordPolicy();

    @Test
    void aceptaUnaContrasenaQueCumpleTodasLasReglas() {
        assertTrue(policy.validate("Autor123!").isEmpty());
    }

    @Test
    void rechazaContrasenaCortaONula() {
        assertEquals(1, policy.validate("Ab1!").size());
        assertEquals(1, policy.validate(null).size());
    }

    @Test
    void exigeDigitoMayusculaYCaracterEspecial() {
        assertEquals(3, policy.validate("abcdefgh").size());
        assertEquals(1, policy.validate("Abcdefg!").size());
        assertEquals(1, policy.validate("abcdef1!").size());
        assertEquals(1, policy.validate("Abcdef12").size());
    }
}
