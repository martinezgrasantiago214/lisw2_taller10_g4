package co.unicauca.saberpro.review.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentacion OpenAPI (Swagger). El servidor es relativo ("/") para que
 * "Try it out" funcione tanto desde este servicio como desde el Swagger del
 * gateway. El boton "Authorize" acepta el token JWT de Keycloak.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("review-service").version("3.0.0")
                        .description("Revision por pares (DDD + arquitectura hexagonal): asignacion de revisores, "
                                + "evaluaciones e historial de revisiones por version."))
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
