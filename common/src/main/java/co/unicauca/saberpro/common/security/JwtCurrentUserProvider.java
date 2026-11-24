package co.unicauca.saberpro.common.security;

import co.unicauca.saberpro.common.web.ForbiddenOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Adaptador de {@link CurrentUserProvider}: lee el JWT ya validado del contexto de seguridad. */
public class JwtCurrentUserProvider implements CurrentUserProvider {

    @Override
    public AuthenticatedUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return KeycloakClaims.toUser(jwt.getClaims());
        }
        throw new ForbiddenOperationException("La peticion no trae un token de acceso valido.");
    }
}
