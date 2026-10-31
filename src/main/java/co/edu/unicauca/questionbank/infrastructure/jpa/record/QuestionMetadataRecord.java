package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA representation of the optional metadata of a question. It is the owning side of the
 * one-to-one association: the foreign key {@code question_id} is NOT NULL and UNIQUE, which
 * guarantees at most one metadata row per question.
 */
@Entity
@Table(name = "question_metadata")
public class QuestionMetadataRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private QuestionRecord question;

    @Column(name = "last_review_date")
    private LocalDateTime lastReviewDate;

    @Column(name = "cognitive_level", length = 80)
    private String cognitiveLevel;

    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    @Column(name = "editorial_observations", length = 2000)
    private String editorialObservations;

    /** Creates an empty record; the mapper fills it. */
    public QuestionMetadataRecord() { }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns owning question.
     *
     * @return owning question
     */
    public QuestionRecord getQuestion() { return question; }
    /**
     * Returns last review date.
     *
     * @return last review date
     */
    public LocalDateTime getLastReviewDate() { return lastReviewDate; }
    /**
     * Returns cognitive level.
     *
     * @return cognitive level
     */
    public String getCognitiveLevel() { return cognitiveLevel; }
    /**
     * Returns estimated time in minutes.
     *
     * @return estimated time in minutes
     */
    public Integer getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
    /**
     * Returns editorial observations.
     *
     * @return editorial observations
     */
    public String getEditorialObservations() { return editorialObservations; }

    /**
     * Sets the owning question.
     *
     * @param question owning question
     */
    void setQuestion(QuestionRecord question) { this.question = question; }
    /**
     * Sets the last review date.
     *
     * @param lastReviewDate last review date
     */
    public void setLastReviewDate(LocalDateTime lastReviewDate) { this.lastReviewDate = lastReviewDate; }
    /**
     * Sets the cognitive level.
     *
     * @param cognitiveLevel cognitive level
     */
    public void setCognitiveLevel(String cognitiveLevel) { this.cognitiveLevel = cognitiveLevel; }
    /**
     * Sets the estimated time in minutes.
     *
     * @param estimatedTimeMinutes estimated time in minutes
     */
    public void setEstimatedTimeMinutes(Integer estimatedTimeMinutes) { this.estimatedTimeMinutes = estimatedTimeMinutes; }
    /**
     * Sets the editorial observations.
     *
     * @param editorialObservations editorial observations
     */
    public void setEditorialObservations(String editorialObservations) { this.editorialObservations = editorialObservations; }
}
