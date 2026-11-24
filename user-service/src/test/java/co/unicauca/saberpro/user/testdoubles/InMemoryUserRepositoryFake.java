package co.unicauca.saberpro.user.testdoubles;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.web.ConflictException;
import co.unicauca.saberpro.user.access.IUserRepository;
import co.unicauca.saberpro.user.domain.User;
import co.unicauca.saberpro.user.domain.UserStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba en memoria del directorio de usuarios (en ejecucion es Keycloak). */
public class InMemoryUserRepositoryFake implements IUserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, String> passwords = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public User create(User newUser, String plainPassword) {
        if (users.containsKey(newUser.getLogin())) {
            throw new ConflictException("Ya existe " + newUser.getLogin());
        }
        User created = new User("id-" + nextId++, newUser.getLogin(), newUser.getFullName(), newUser.getEmail(),
                newUser.getRole(), UserStatus.ACTIVO);
        users.put(created.getLogin(), created);
        passwords.put(created.getLogin(), plainPassword);
        return created;
    }

    /** Para preparar datos: un usuario ya existente (p. ej. desactivado en la consola de Keycloak). */
    public void add(User user) {
        users.put(user.getLogin(), user);
    }

    public String passwordOf(String login) {
        return passwords.get(login);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(users.get(login));
    }

    @Override
    public List<User> list() {
        return new ArrayList<>(users.values());
    }

    @Override
    public List<User> listByRole(Role role) {
        return users.values().stream().filter(u -> u.getRole() == role).toList();
    }
}
