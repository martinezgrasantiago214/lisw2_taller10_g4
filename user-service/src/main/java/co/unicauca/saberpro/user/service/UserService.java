package co.unicauca.saberpro.user.service;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.access.IUserRepository;
import co.unicauca.saberpro.user.domain.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Logica de negocio de "Gestion de usuarios": registro y consulta de
 * usuarios, administradores y revisores.
 *
 * Tercer corte: la autenticacion (login, tokens JWT) la hace Keycloak, no
 * este servicio. Aqui quedan las reglas del registro antes de crear el
 * usuario en Keycloak, y las consultas que usan los demas servicios.
 *
 * DIP: depende de dos abstracciones (IUserRepository e IPasswordPolicy),
 * inyectadas por constructor.
 */
public class UserService {

    /** Login: 3 a 30 caracteres, letras minusculas, digitos, punto, guion o guion bajo. */
    private static final Pattern LOGIN = Pattern.compile("[a-z0-9._-]{3,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final IUserRepository repository;
    private final IPasswordPolicy passwordPolicy;

    public UserService(IUserRepository repository, IPasswordPolicy passwordPolicy) {
        this.repository = repository;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * Registra un usuario en Keycloak con su rol.
     *
     * Reglas: login valido y libre, nombre y apellido (Keycloak exige ambos),
     * correo valido (a el llegan las notificaciones), rol obligatorio,
     * contrasena segun la politica, y el rol Administrador solo lo puede dar
     * otro administrador.
     *
     * @param requestedByAdmin true si quien registra es un administrador autenticado.
     */
    public OperationResult register(String login, String fullName, String email, Role role, String plainPassword,
                                    boolean requestedByAdmin) {
        List<String> errors = new ArrayList<>();
        String cleanLogin = login == null ? "" : login.trim().toLowerCase();
        if (cleanLogin.isEmpty()) {
            errors.add("El nombre de usuario (login) es obligatorio.");
        } else if (!LOGIN.matcher(cleanLogin).matches()) {
            errors.add("El login debe tener de 3 a 30 caracteres: letras, digitos, punto, guion o guion bajo.");
        }
        String cleanName = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (cleanName.isEmpty()) {
            errors.add("El nombre completo es obligatorio.");
        } else if (!cleanName.contains(" ")) {
            errors.add("Escriba nombre y apellido.");
        }
        String cleanEmail = email == null ? "" : email.trim();
        if (!EMAIL.matcher(cleanEmail).matches()) {
            errors.add("Debe indicar un correo electronico valido: a el llegan las notificaciones.");
        }
        if (role == null) {
            errors.add("Debe seleccionar un rol.");
        } else if (role == Role.ADMINISTRADOR && !requestedByAdmin) {
            errors.add("Solo un administrador puede registrar otro administrador.");
        }
        errors.addAll(passwordPolicy.validate(plainPassword));
        if (!errors.isEmpty()) {
            return OperationResult.fail(errors);
        }
        if (repository.existsByLogin(cleanLogin)) {
            return OperationResult.fail("Ya existe un usuario registrado con ese login.");
        }
        repository.create(User.newUser(cleanLogin, cleanName, cleanEmail, role), plainPassword);
        return OperationResult.ok();
    }

    public Optional<User> findByLogin(String login) {
        return login == null || login.isBlank() ? Optional.empty() : repository.findByLogin(login.trim());
    }

    public List<User> listUsers() {
        return repository.list();
    }

    public List<User> listByRole(Role role) {
        return repository.listByRole(role);
    }

    /** HU04: el administrador solo puede asignar usuarios activos con rol REVISOR. */
    public List<User> listReviewers() {
        return repository.listByRole(Role.REVISOR).stream().filter(User::isActive).toList();
    }
}
