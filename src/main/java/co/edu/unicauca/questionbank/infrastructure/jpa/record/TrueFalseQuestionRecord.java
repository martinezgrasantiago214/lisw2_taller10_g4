package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;

/** JOINED subtype mapped to {@code true_false_questions} (PK/FK {@code question_id}). */
@Entity
@Table(name = "true_false_questions")
@PrimaryKeyJoinColumn(name = "question_id")
public class TrueFalseQuestionRecord extends QuestionRecord {
    @Column(name = "expected_value", nullable = false)
    private boolean expectedValue;

    /** Creates an empty record; the mapper fills it. */
    public TrueFalseQuestionRecord() { }

    /**
     * Returns expected truth value.
     *
     * @return expected truth value
     */
    public boolean getExpectedValue() { return expectedValue; }
    /**
     * Sets the expected truth value.
     *
     * @param expectedValue expected truth value
     */
    public void setExpectedValue(boolean expectedValue) { this.expectedValue = expectedValue; }
}
