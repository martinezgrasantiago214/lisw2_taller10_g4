package co.unicauca.saberpro.user.web;

import co.unicauca.saberpro.common.contract.ApiError;
import co.unicauca.saberpro.user.access.IdentityProviderUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/** Keycloak caido: 503 Service Unavailable (no es un error del cliente ni un 500 generico). */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class UserApiExceptionHandler {

    @ExceptionHandler(IdentityProviderUnavailableException.class)
    public ResponseEntity<ApiError> handleUnavailable(IdentityProviderUnavailableException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(new ApiError(LocalDateTime.now().toString(), status.value(),
                status.getReasonPhrase(), List.of(ex.getMessage()), req.getRequestURI()));
    }
}
