package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;

/** JPA representation of a topic, mapped to the {@code topics} table. */
@Entity
@Table(name = "topics")
public class TopicRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(name = "saber_pro_module", nullable = false, length = 80)
    private String module;

    /** Constructor required by JPA. */
    protected TopicRecord() { }

    /**
     * Creates a TopicRecord with the given values.
     *
     * @param name   unique topic name
     * @param module Saber PRO module
     */
    public TopicRecord(String name, String module) {
        this.name = name;
        this.module = module;
    }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns name.
     *
     * @return name
     */
    public String getName() { return name; }
    /**
     * Returns Saber PRO module.
     *
     * @return Saber PRO module
     */
    public String getModule() { return module; }
    /**
     * Sets the new name.
     *
     * @param name new name
     */
    public void setName(String name) { this.name = name; }
    /**
     * Sets the new module.
     *
     * @param module new module
     */
    public void setModule(String module) { this.module = module; }
}
