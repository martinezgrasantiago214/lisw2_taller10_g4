package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.application.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates domain and application exceptions into HTTP problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Creates the handler. */
    public ApiExceptionHandler() { }

    /**
     * Returns 404 Not Found.
     *
     * @param e missing resource
     * @return 404 Not Found
     */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Returns 400 Bad Request.
     *
     * @param e invalid input
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Returns 409 Conflict.
     *
     * @param e business rule violation, such as an invalid lifecycle transition
     * @return 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Raised by the database, for example when deleting a question that belongs to a quiz.
     *
     * @param e constraint violation
     * @return 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The operation violates a database constraint (is the question used in a quiz?)");
    }
}
