package co.unicauca.saberpro.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * Convierte un JWT de Keycloak en la autenticacion de Spring Security:
 * cada rol del realm pasa a ser una autoridad {@code ROLE_<rol>} (asi
 * funcionan {@code hasRole('ADMINISTRADOR')} y {@code @PreAuthorize}) y el
 * nombre del usuario es su {@code preferred_username}.
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = KeycloakClaims.realmRoles(jwt.getClaims()).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, KeycloakClaims.username(jwt.getClaims()));
    }
}
