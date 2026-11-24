package co.unicauca.saberpro.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway (puerto 8080): unico punto de entrada del frontend al backend.
 * Rutas en application.yml; seguridad (JWT de Keycloak) en GatewaySecurityConfig.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
