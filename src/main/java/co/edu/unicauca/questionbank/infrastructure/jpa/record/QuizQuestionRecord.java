package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;

/**
 * Association entity between quizzes and questions. It replaces a plain many-to-many because
 * the link needs its own attribute (the position) and its own unique constraints.
 */
@Entity
@Table(name = "quiz_questions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_quiz_question", columnNames = {"quiz_id", "question_id"}),
    @UniqueConstraint(name = "uk_quiz_position", columnNames = {"quiz_id", "question_order"})
})
public class QuizQuestionRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizRecord quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionRecord question;

    @Column(name = "question_order", nullable = false)
    private int position;

    /** Constructor required by JPA. */
    protected QuizQuestionRecord() { }

    /**
     * Creates a QuizQuestionRecord with the given values.
     *
     * @param quiz     owning quiz
     * @param question linked question
     * @param position 1-based position
     */
    public QuizQuestionRecord(QuizRecord quiz, QuestionRecord question, int position) {
        this.quiz = quiz;
        this.question = question;
        this.position = position;
    }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns owning quiz.
     *
     * @return owning quiz
     */
    public QuizRecord getQuiz() { return quiz; }
    /**
     * Returns linked question.
     *
     * @return linked question
     */
    public QuestionRecord getQuestion() { return question; }
    /**
     * Returns 1-based position.
     *
     * @return 1-based position
     */
    public int getPosition() { return position; }
}
