package co.unicauca.saberpro.user.domain;

import co.unicauca.saberpro.common.contract.Role;

import java.util.Objects;

/**
 * Usuario del sistema (autor de preguntas, administrador o revisor).
 *
 * Tercer corte: la identidad, las credenciales y los roles los administra
 * Keycloak. Esta entidad es la vista que el sistema necesita de un usuario;
 * la contrasena ya no pasa por aqui (ni en claro ni cifrada).
 */
public class User {

    private final String id;
    private final String login;
    private final String fullName;
    private final String email;
    private final Role role;
    private final UserStatus status;

    public User(String id, String login, String fullName, String email, Role role, UserStatus status) {
        this.id = id;
        this.login = Objects.requireNonNull(login, "login");
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status != null ? status : UserStatus.ACTIVO;
    }

    /** Usuario nuevo, antes de existir en Keycloak (aun no tiene id). */
    public static User newUser(String login, String fullName, String email, Role role) {
        return new User(null, login, fullName, email, role, UserStatus.ACTIVO);
    }

    public String getId() { return id; }
    public String getLogin() { return login; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }

    public boolean isActive() {
        return status == UserStatus.ACTIVO;
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }

    /** Nombre (primera palabra) para el atributo firstName de Keycloak. */
    public String firstName() {
        String name = fullName == null ? "" : fullName.trim();
        int space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }

    /** Apellidos (el resto del nombre completo) para el atributo lastName de Keycloak. */
    public String lastName() {
        String name = fullName == null ? "" : fullName.trim();
        int space = name.indexOf(' ');
        return space < 0 ? "" : name.substring(space + 1).trim();
    }

    @Override
    public String toString() {
        return fullName + " (" + login + ") - " + role;
    }
}
