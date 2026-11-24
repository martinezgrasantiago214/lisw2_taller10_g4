package co.unicauca.saberpro.question.config;

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
 * "Try it out" funcione tanto desde este servicio como desde el Swagger del gateway.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("question-service").version("3.0.0")
                        .description("Gestion, validacion estructural y ciclo de vida de las preguntas."))
                .servers(List.of(new Server().url("/")))
                // Tercer corte: boton "Authorize" de Swagger para pegar el token JWT de Keycloak.
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
