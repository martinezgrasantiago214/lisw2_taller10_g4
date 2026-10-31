package co.edu.unicauca.questionbank.domain.model;

/**
 * Topic assessed by a question, associated with a Saber PRO module
 * (for example, "Percentages" in "Quantitative reasoning").
 */
public class Topic {
    private Long id;
    private final String name;
    private final String module;

    /**
     * Creates a topic.
     *
     * @param id     persistent identifier, or {@code null} for a new topic
     * @param name   topic name, required
     * @param module Saber PRO module, required
     * @throws IllegalArgumentException if a required value is missing
     */
    public Topic(Long id, String name, String module) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Topic name is required");
        if (module == null || module.isBlank()) throw new IllegalArgumentException("Topic module is required");
        this.id = id;
        this.name = name;
        this.module = module;
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
     * Returns topic name.
     *
     * @return topic name
     */
    public String getName() { return name; }

    /**
     * Returns Saber PRO module.
     *
     * @return Saber PRO module
     */
    public String getModule() { return module; }
}
