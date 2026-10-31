package co.edu.unicauca.questionbank.domain.model;

/** Generic Saber PRO competency that a question can assess. */
public class Competency {
    private Long id;
    private final String code;
    private final String name;

    /**
     * Creates a competency.
     *
     * @param id   persistent identifier, or {@code null} for a new competency
     * @param code short unique code, required
     * @param name descriptive name, required
     * @throws IllegalArgumentException if a required value is missing
     */
    public Competency(Long id, String code, String name) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Competency code is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Competency name is required");
        this.id = id;
        this.code = code;
        this.name = name;
    }

    /**
     * Returns persistent identifier.
     *
     * @return persistent identifier
     */
    public Long getId() { return id; }

    /**
     * Sets the persistent identifier assigned by the repository.
     *
     * @param id persistent identifier assigned by the repository
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns unique code.
     *
     * @return unique code
     */
    public String getCode() { return code; }

    /**
     * Returns descriptive name.
     *
     * @return descriptive name
     */
    public String getName() { return name; }
}
