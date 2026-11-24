package co.unicauca.saberpro.user.service;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.domain.User;
import co.unicauca.saberpro.user.domain.UserStatus;
import co.unicauca.saberpro.user.testdoubles.InMemoryUserRepositoryFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas del registro y consultas del user-service, con un directorio en memoria en lugar de Keycloak. */
class UserServiceTest {

    private InMemoryUserRepositoryFake repository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepositoryFake();
        userService = new UserService(repository, new DefaultPasswordPolicy());
    }

    @Test
    void registraUnUsuarioValidoEnElDirectorio() {
        OperationResult result = userService.register(" Autor1 ", "Autor   Uno", " autor1@x.co ",
                Role.AUTOR_PREGUNTAS, "Autor123!", false);

        assertTrue(result.isSuccess(), result.getErrorsAsText());
        User created = userService.findByLogin("autor1").orElseThrow();
        assertEquals("Autor Uno", created.getFullName());
        assertEquals("autor1@x.co", created.getEmail());
        assertEquals("Autor123!", repository.passwordOf("autor1"), "la clave va a Keycloak, no se guarda aqui");
    }

    @Test
    void reportaTodosLosErroresDeUnaVez() {
        OperationResult result = userService.register("", "Autor", "no-es-correo", null, "debil", false);

        assertFalse(result.isSuccess());
        List<String> errors = result.getErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("login")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("nombre y apellido")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("correo")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("rol")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("contrasena")));
        assertTrue(repository.list().isEmpty());
    }

    @Test
    void elLoginTieneUnFormatoValido() {
        assertFalse(userService.register("a b", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, "Autor123!", false)
                .isSuccess());
        assertFalse(userService.register("ab", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, "Autor123!", false)
                .isSuccess());
    }

    @Test
    void rechazaContrasenaQueNoCumpleLaPolitica() {
        OperationResult result = userService.register("autor1", "Autor Uno", "autor1@x.co",
                Role.AUTOR_PREGUNTAS, "sinmayuscula1!", false);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorsAsText().contains("mayuscula"));
    }

    @Test
    void rechazaLoginDuplicado() {
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, "Autor123!", false);
        OperationResult result = userService.register("autor1", "Otro Autor", "b@x.co", Role.AUTOR_PREGUNTAS,
                "Autor123!", false);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorsAsText().contains("Ya existe"));
    }

    @Test
    void soloUnAdministradorPuedeCrearOtroAdministrador() {
        assertFalse(userService.register("admin2", "Admin Dos", "a2@x.co", Role.ADMINISTRADOR, "Admin123!", false)
                .isSuccess());
        assertTrue(userService.register("admin2", "Admin Dos", "a2@x.co", Role.ADMINISTRADOR, "Admin123!", true)
                .isSuccess());
    }

    @Test
    void listReviewersSoloDevuelveRevisoresActivos() {
        userService.register("rev1", "Revisor Uno", "r@x.co", Role.REVISOR, "Revisor123!", false);
        userService.register("autor1", "Autor Uno", "a@x.co", Role.AUTOR_PREGUNTAS, "Autor123!", false);
        repository.add(new User("x", "rev9", "Revisor Inactivo", "r9@x.co", Role.REVISOR, UserStatus.INACTIVO));

        List<User> reviewers = userService.listReviewers();

        assertEquals(List.of("rev1"), reviewers.stream().map(User::getLogin).toList());
        assertEquals(2, userService.listByRole(Role.REVISOR).size());
        assertEquals(3, userService.listUsers().size());
    }

    @Test
    void buscarSinLoginNoConsultaElDirectorio() {
        assertTrue(userService.findByLogin(null).isEmpty());
        assertTrue(userService.findByLogin(" ").isEmpty());
    }
}
