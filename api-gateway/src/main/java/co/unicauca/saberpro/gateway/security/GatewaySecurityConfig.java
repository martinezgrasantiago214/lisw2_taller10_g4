package co.unicauca.saberpro.gateway.security;

import co.unicauca.saberpro.common.security.KeycloakJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Seguridad del API Gateway (tercer corte): primera barrera de la defensa en
 * profundidad.
 *
 * <ol>
 *   <li>Sin token valido de Keycloak no se enruta nada hacia /api/** (401):
 *       una peticion anonima nunca llega a los microservicios.</li>
 *   <li>Reglas gruesas por rol en las operaciones mas sensibles (403).</li>
 *   <li>El header Authorization se reenvia tal cual: cada microservicio vuelve
 *       a validar el token y aplica sus reglas finas (@PreAuthorize y dominio).</li>
 * </ol>
 *
 * Publico: auto-registro (POST /api/users), salud y documentacion Swagger.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private static final String ADMIN = "ADMINISTRADOR";
    private static final String AUTHOR = "AUTOR_PREGUNTAS";
    private static final String REVIEWER = "REVISOR";

    @Bean
    public SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**", "/actuator/info", "/swagger-ui.html", "/swagger-ui/**",
                                "/webjars/**", "/v3/api-docs/**", "/docs/**").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // HU04: solo administradores ven pendientes y asignan revisores.
                        .pathMatchers("/api/reviews/pending-questions").hasRole(ADMIN)
                        .pathMatchers(HttpMethod.POST, "/api/reviews/assignments").hasRole(ADMIN)
                        // HU05: solo revisores evaluan.
                        .pathMatchers(HttpMethod.POST, "/api/reviews/evaluations").hasRole(REVIEWER)
                        // HU01, HU02, HU03 y HU06: solo autores crean, editan, envian y corrigen preguntas.
                        .pathMatchers(HttpMethod.POST, "/api/questions", "/api/questions/*/revisions").hasRole(AUTHOR)
                        .pathMatchers(HttpMethod.PUT, "/api/questions/*").hasRole(AUTHOR)
                        .pathMatchers(HttpMethod.PATCH, "/api/questions/*/submit-review").hasRole(AUTHOR)
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new ReactiveJwtAuthenticationConverterAdapter(new KeycloakJwtAuthenticationConverter()))));
        return http.build();
    }
}
