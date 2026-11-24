package co.unicauca.saberpro.user.access;

/** Keycloak no respondio o rechazo las credenciales del servicio. Se responde 503. */
public class IdentityProviderUnavailableException extends RuntimeException {

    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
