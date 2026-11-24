package co.unicauca.saberpro.user.config;

import co.unicauca.saberpro.common.security.KeycloakClientCredentials;
import co.unicauca.saberpro.common.security.ServiceTokenProvider;
import co.unicauca.saberpro.user.access.IUserRepository;
import co.unicauca.saberpro.user.access.KeycloakUserRepository;
import co.unicauca.saberpro.user.service.DefaultPasswordPolicy;
import co.unicauca.saberpro.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Ensambla las dependencias del servicio (DIP): las clases de negocio no
 * conocen Spring ni Keycloak. Tercer corte: el directorio de usuarios es el
 * Admin REST API de Keycloak, al que se llama con el token del cliente
 * confidencial saberpro-backend (client credentials).
 */
@Configuration
public class UserServiceConfig {

    @Bean
    public ServiceTokenProvider serviceTokenProvider(@Value("${saberpro.security.service-client.token-uri}") String tokenUri,
                                                     @Value("${saberpro.security.service-client.id}") String clientId,
                                                     @Value("${saberpro.security.service-client.secret}") String secret) {
        return KeycloakClientCredentials.provider(tokenUri, clientId, secret);
    }

    @Bean
    public IUserRepository userRepository(@Value("${saberpro.keycloak.admin-url}") String adminUrl,
                                          ServiceTokenProvider tokens) {
        RestClient admin = RestClient.builder()
                .baseUrl(adminUrl)
                .requestInterceptor(KeycloakClientCredentials.bearerInterceptor(tokens))
                .build();
        return new KeycloakUserRepository(admin);
    }

    @Bean
    public UserService userService(IUserRepository userRepository) {
        return new UserService(userRepository, new DefaultPasswordPolicy());
    }
}
