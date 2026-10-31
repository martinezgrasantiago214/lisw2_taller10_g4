package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;

/** JOINED subtype mapped to {@code open_questions} (PK/FK {@code question_id}). */
@Entity
@Table(name = "open_questions")
@PrimaryKeyJoinColumn(name = "question_id")
public class OpenQuestionRecord extends QuestionRecord {
    @Column(name = "expected_answer", length = 2000)
    private String expectedAnswer;

    @Column(name = "max_words", nullable = false)
    private int maxWords;

    /** Creates an empty record; the mapper fills it. */
    public OpenQuestionRecord() { }

    /**
     * Returns grading guidelines.
     *
     * @return grading guidelines
     */
    public String getExpectedAnswer() { return expectedAnswer; }
    /**
     * Returns maximum number of words.
     *
     * @return maximum number of words
     */
    public int getMaxWords() { return maxWords; }
    /**
     * Sets the grading guidelines.
     *
     * @param expectedAnswer grading guidelines
     */
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    /**
     * Sets the maximum number of words.
     *
     * @param maxWords maximum number of words
     */
    public void setMaxWords(int maxWords) { this.maxWords = maxWords; }
}
