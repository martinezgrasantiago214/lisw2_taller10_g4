package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Root of the relational question hierarchy.
 *
 * <p>{@code InheritanceType.JOINED} creates the base table {@code questions} with the common
 * columns and one table per subtype whose primary key is also a foreign key to
 * {@code questions.id}.</p>
 */
@Entity
@Table(name = "questions")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class QuestionRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DifficultyColumn difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusColumn status;

    /** Many-to-one: owning side; stores the {@code author_id} foreign key. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserRecord author;

    /** Many-to-one: owning side; stores the {@code topic_id} foreign key. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private TopicRecord topic;

    /** Many-to-many: owning side; defines the {@code question_competencies} join table. */
    @ManyToMany
    @JoinTable(name = "question_competencies",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "competency_id"))
    private Set<CompetencyRecord> competencies = new HashSet<>();

    /** One-to-one: inverse side; the foreign key lives in {@code question_metadata}. */
    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private QuestionMetadataRecord metadata;

    /** Constructor required by JPA. */
    protected QuestionRecord() { }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns statement.
     *
     * @return statement
     */
    public String getStatement() { return statement; }
    /**
     * Returns difficulty.
     *
     * @return difficulty
     */
    public DifficultyColumn getDifficulty() { return difficulty; }
    /**
     * Returns lifecycle state.
     *
     * @return lifecycle state
     */
    public StatusColumn getStatus() { return status; }
    /**
     * Returns author.
     *
     * @return author
     */
    public UserRecord getAuthor() { return author; }
    /**
     * Returns topic.
     *
     * @return topic
     */
    public TopicRecord getTopic() { return topic; }
    /**
     * Returns managed set of competencies.
     *
     * @return managed set of competencies
     */
    public Set<CompetencyRecord> getCompetencies() { return competencies; }
    /**
     * Returns metadata, or {@code null}.
     *
     * @return metadata, or {@code null}
     */
    public QuestionMetadataRecord getMetadata() { return metadata; }

    /**
     * Sets the new statement.
     *
     * @param statement new statement
     */
    public void setStatement(String statement) { this.statement = statement; }
    /**
     * Sets the new difficulty.
     *
     * @param difficulty new difficulty
     */
    public void setDifficulty(DifficultyColumn difficulty) { this.difficulty = difficulty; }
    /**
     * Sets the new lifecycle state.
     *
     * @param status new lifecycle state
     */
    public void setStatus(StatusColumn status) { this.status = status; }
    /**
     * Sets the author.
     *
     * @param author author
     */
    public void setAuthor(UserRecord author) { this.author = author; }
    /**
     * Sets the topic.
     *
     * @param topic topic
     */
    public void setTopic(TopicRecord topic) { this.topic = topic; }

    /**
     * Sets the metadata keeping both sides of the one-to-one association synchronized.
     * Setting {@code null} lets orphan removal delete the previous row.
     *
     * @param newMetadata metadata, or {@code null}
     */
    public void setMetadata(QuestionMetadataRecord newMetadata) {
        if (this.metadata == newMetadata) return;
        this.metadata = newMetadata;
        if (newMetadata != null) newMetadata.setQuestion(this);
    }

    /**
     * Replaces the contents of the managed collection without replacing the collection itself,
     * so Hibernate can compute the minimal changes to the join table.
     *
     * @param newCompetencies new competencies
     */
    public void replaceCompetencies(Set<CompetencyRecord> newCompetencies) {
        competencies.retainAll(newCompetencies);
        competencies.addAll(newCompetencies);
    }
}
