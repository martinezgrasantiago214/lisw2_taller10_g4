package co.unicauca.saberpro.common.security;

/**
 * Puerto para conocer al usuario autenticado de la peticion en curso (DIP).
 *
 * En ejecucion lo implementa {@link JwtCurrentUserProvider}, que lee el token
 * JWT validado por Spring Security; en las pruebas se reemplaza por un doble
 * que devuelve un usuario fijo, sin necesidad de Keycloak.
 */
@FunctionalInterface
public interface CurrentUserProvider {

    /**
     * @return el usuario autenticado.
     * @throws co.unicauca.saberpro.common.web.ForbiddenOperationException si la peticion no trae un token valido.
     */
    AuthenticatedUser current();
}
