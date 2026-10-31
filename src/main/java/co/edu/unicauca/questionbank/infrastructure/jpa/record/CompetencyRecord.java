package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;

/**
 * JPA representation of a competency, mapped to the {@code competencies} table.
 * It is the non-owning side of the many-to-many association with questions.
 */
@Entity
@Table(name = "competencies")
public class CompetencyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    /** Constructor required by JPA. */
    protected CompetencyRecord() { }

    /**
     * Creates a CompetencyRecord with the given values.
     *
     * @param code unique code
     * @param name descriptive name
     */
    public CompetencyRecord(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns code.
     *
     * @return code
     */
    public String getCode() { return code; }
    /**
     * Returns name.
     *
     * @return name
     */
    public String getName() { return name; }
    /**
     * Sets the new code.
     *
     * @param code new code
     */
    public void setCode(String code) { this.code = code; }
    /**
     * Sets the new name.
     *
     * @param name new name
     */
    public void setName(String name) { this.name = name; }
}
