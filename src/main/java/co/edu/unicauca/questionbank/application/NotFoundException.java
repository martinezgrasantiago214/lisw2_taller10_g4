package co.edu.unicauca.questionbank.application;

/** Thrown when a requested resource does not exist. */
public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a NotFoundException with the given values.
     *
     * @param message description of the missing resource
     */
    public NotFoundException(String message) { super(message); }
}
