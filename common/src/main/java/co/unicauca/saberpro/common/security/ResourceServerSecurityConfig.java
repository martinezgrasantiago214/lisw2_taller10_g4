package co.unicauca.saberpro.common.security;

import co.unicauca.saberpro.common.contract.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seguridad comun de los microservicios con API REST (tercer corte).
 *
 * Cada servicio es un "resource server" OAuth2: solo acepta peticiones con un
 * token JWT firmado por Keycloak (realm saberpro). La firma se valida con las
 * llaves publicas del realm (JWKS), y tambien el emisor (iss), la audiencia
 * (aud = saberpro-api) y la vigencia; todo eso lo configuran las propiedades
 * spring.security.oauth2.resourceserver.jwt.* de cada servicio.
 *
 * Autorizacion en dos niveles:
 * <ol>
 *   <li>Aqui: todo /api/** exige un token valido (401 si falta o es invalido).</li>
 *   <li>En cada controlador: {@code @PreAuthorize} con el rol que pide cada
 *       caso de uso (403 si el rol no alcanza). Las reglas finas (p. ej.
 *       "solo el autor edita su pregunta") siguen en el dominio.</li>
 * </ol>
 *
 * Sin estado (stateless) y sin CSRF: no hay sesion ni cookies, cada peticion
 * trae su token. Asi cualquier instancia del servicio atiende cualquier
 * peticion, lo que permite escalar horizontalmente.
 *
 * Cada servicio la importa con {@code @Import}. Un servicio puede abrir rutas
 * POST publicas con la propiedad {@code saberpro.security.public-post-paths}
 * (el user-service la usa para el auto-registro).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerSecurityConfig {

    /**
     * Rutas que nunca piden token: salud del servicio, documentacion OpenAPI y
     * /error (si no, un 400 o 404 se convertiria en 401 al despachar el error).
     */
    static final String[] ALWAYS_PUBLIC = {
        "/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error"
    };

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                                      @Value("${saberpro.security.public-post-paths:}")
                                                      String[] publicPostPaths) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, ex) ->
                writeError(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                        "Debe iniciar sesion: la peticion no trae un token de acceso valido o ya vencio.");
        AccessDeniedHandler forbidden = (request, response, ex) ->
                writeError(objectMapper, request, response, HttpStatus.FORBIDDEN,
                        "Su rol no tiene permiso para esta operacion.");

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(ALWAYS_PUBLIC).permitAll();
                    for (String path : publicPostPaths) {
                        if (path != null && !path.isBlank()) {
                            auth.requestMatchers(HttpMethod.POST, path.trim()).permitAll();
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter()))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden));
        return http.build();
    }

    @Bean
    public CurrentUserProvider currentUserProvider() {
        return new JwtCurrentUserProvider();
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiError body = new ApiError(LocalDateTime.now().toString(), status.value(), status.getReasonPhrase(),
                List.of(message), request.getRequestURI());
        mapper.writeValue(response.getOutputStream(), body);
    }
}
