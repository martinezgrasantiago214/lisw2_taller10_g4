package co.edu.unicauca.questionbank.domain.model;

import java.time.LocalDateTime;

/**
 * Optional complementary information of a question (one-to-one relationship).
 * Pure domain value object without persistence annotations.
 */
public class QuestionMetadata {
    private final LocalDateTime lastReviewDate;
    private final String cognitiveLevel;
    private final Integer estimatedTimeMinutes;
    private final String editorialObservations;

    /**
     * Creates question metadata. Every value is optional.
     *
     * @param lastReviewDate        date of the last review
     * @param cognitiveLevel        cognitive level (for example, ANALYSIS); not blank if present
     * @param estimatedTimeMinutes  estimated answering time; positive if present
     * @param editorialObservations editorial notes
     * @throws IllegalArgumentException if a present value is invalid
     */
    public QuestionMetadata(LocalDateTime lastReviewDate, String cognitiveLevel,
                            Integer estimatedTimeMinutes, String editorialObservations) {
        if (cognitiveLevel != null && cognitiveLevel.isBlank())
            throw new IllegalArgumentException("Cognitive level cannot be blank");
        if (estimatedTimeMinutes != null && estimatedTimeMinutes <= 0)
            throw new IllegalArgumentException("Estimated time must be greater than zero");
        this.lastReviewDate = lastReviewDate;
        this.cognitiveLevel = cognitiveLevel;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.editorialObservations = editorialObservations;
    }

    /**
     * Returns date of the last review, or {@code null}.
     *
     * @return date of the last review, or {@code null}
     */
    public LocalDateTime getLastReviewDate() { return lastReviewDate; }

    /**
     * Returns cognitive level, or {@code null}.
     *
     * @return cognitive level, or {@code null}
     */
    public String getCognitiveLevel() { return cognitiveLevel; }

    /**
     * Returns estimated answering time in minutes, or {@code null}.
     *
     * @return estimated answering time in minutes, or {@code null}
     */
    public Integer getEstimatedTimeMinutes() { return estimatedTimeMinutes; }

    /**
     * Returns editorial observations, or {@code null}.
     *
     * @return editorial observations, or {@code null}
     */
    public String getEditorialObservations() { return editorialObservations; }
}
