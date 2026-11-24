package co.unicauca.saberpro.notification.config;

import co.unicauca.saberpro.common.security.KeycloakClientCredentials;
import co.unicauca.saberpro.common.security.ServiceTokenProvider;
import co.unicauca.saberpro.notification.application.NotificationService;
import co.unicauca.saberpro.notification.channel.OutboxFileChannel;
import co.unicauca.saberpro.notification.channel.SmtpEmailChannelAdapter;
import co.unicauca.saberpro.notification.channel.SmtpMailClient;
import co.unicauca.saberpro.notification.directory.UserDirectory;
import co.unicauca.saberpro.notification.directory.UserServiceRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.io.File;

/** Ensambla el notification-service. */
@Configuration
public class NotificationServiceConfig {

    /** Token de servicio (client credentials) para consultar el user-service. */
    @Bean
    public ServiceTokenProvider serviceTokenProvider(@Value("${saberpro.security.service-client.token-uri}") String tokenUri,
                                                     @Value("${saberpro.security.service-client.id}") String clientId,
                                                     @Value("${saberpro.security.service-client.secret}") String secret) {
        return KeycloakClientCredentials.provider(tokenUri, clientId, secret);
    }

    @Bean
    public UserDirectory userDirectory(@Value("${saberpro.user-service-url}") String userServiceUrl,
                                       ServiceTokenProvider tokens) {
        return new UserServiceRestClient(RestClient.builder()
                .baseUrl(userServiceUrl)
                .requestInterceptor(KeycloakClientCredentials.bearerInterceptor(tokens))
                .build());
    }

    @Bean
    public NotificationService notificationService(UserDirectory userDirectory,
                                                   @Value("${saberpro.outbox-dir}") String outboxDir) {
        return new NotificationService(new SmtpEmailChannelAdapter(SmtpMailClient.fromClasspath()),
                new OutboxFileChannel(new File(outboxDir)), userDirectory);
    }
}
