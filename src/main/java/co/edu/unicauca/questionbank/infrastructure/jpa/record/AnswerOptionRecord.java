package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;

/**
 * JPA representation of an answer option. Owning side of the association with its question:
 * the non-null foreign key {@code question_id} prevents options without a question.
 */
@Entity
@Table(name = "answer_options")
public class AnswerOptionRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1)
    private String label;

    @Column(name = "option_text", nullable = false, length = 1000)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private MultipleChoiceQuestionRecord question;

    /** Constructor required by JPA. */
    protected AnswerOptionRecord() { }

    /**
     * Creates an AnswerOptionRecord with the given values.
     *
     * @param label   one-letter label
     * @param text    option text
     * @param correct whether it is the correct answer
     */
    public AnswerOptionRecord(String label, String text, boolean correct) {
        this.label = label;
        this.text = text;
        this.correct = correct;
    }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns label.
     *
     * @return label
     */
    public String getLabel() { return label; }
    /**
     * Returns text.
     *
     * @return text
     */
    public String getText() { return text; }
    /**
     * Returns {@code true} if it is the correct answer.
     *
     * @return {@code true} if it is the correct answer
     */
    public boolean isCorrect() { return correct; }
    /**
     * Returns owning question.
     *
     * @return owning question
     */
    public MultipleChoiceQuestionRecord getQuestion() { return question; }
    /**
     * Sets the owning question; package-private so only the parent sets it.
     *
     * @param question owning question; package-private so only the parent sets it
     */
    void setQuestion(MultipleChoiceQuestionRecord question) { this.question = question; }
}
