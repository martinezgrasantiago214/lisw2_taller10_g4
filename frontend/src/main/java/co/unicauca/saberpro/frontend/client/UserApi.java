package co.unicauca.saberpro.frontend.client;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.frontend.model.Models.UserView;

import java.util.List;

/** Operaciones del user-service (via gateway). */
public interface UserApi {

    /** Inicia sesion en Keycloak y devuelve los datos del usuario autenticado. */
    UserView login(String login, String password);

    /** Cierra la sesion (olvida los tokens). */
    void logout();

    UserView register(String login, String fullName, String email, Role role, String password);

    List<UserView> listReviewers();
}
