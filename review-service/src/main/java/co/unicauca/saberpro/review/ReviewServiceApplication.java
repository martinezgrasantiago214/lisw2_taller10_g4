package co.unicauca.saberpro.review;

import co.unicauca.saberpro.common.messaging.MessagingConfig;
import co.unicauca.saberpro.common.security.ResourceServerSecurityConfig;
import co.unicauca.saberpro.common.security.SecurityExceptionHandler;
import co.unicauca.saberpro.common.web.GlobalApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Microservicio de revision por pares (puerto 8083). Tercer corte: arquitectura
 * limpia con DDD (agregado QuestionReview) y hexagonal (puertos y adaptadores).
 */
@SpringBootApplication
@Import({MessagingConfig.class, GlobalApiExceptionHandler.class, ResourceServerSecurityConfig.class,
        SecurityExceptionHandler.class})
public class ReviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }
}
