package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** JPA representation of a quiz, mapped to the {@code quizzes} table. */
@Entity
@Table(name = "quizzes")
public class QuizRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 2000)
    private String description;

    /** One-to-many to the association entity that keeps the question order. */
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuizQuestionRecord> questions = new ArrayList<>();

    /** Creates an empty record; the repository fills it. */
    public QuizRecord() { }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns title.
     *
     * @return title
     */
    public String getTitle() { return title; }
    /**
     * Returns description.
     *
     * @return description
     */
    public String getDescription() { return description; }
    /**
     * Returns managed list of question links, ordered by position.
     *
     * @return managed list of question links, ordered by position
     */
    public List<QuizQuestionRecord> getQuestions() { return questions; }
    /**
     * Sets the title.
     *
     * @param title title
     */
    public void setTitle(String title) { this.title = title; }
    /**
     * Sets the description.
     *
     * @param description description
     */
    public void setDescription(String description) { this.description = description; }
    /** Removes every question link; orphan removal deletes the rows. */
    public void clearQuestions() { questions.clear(); }

    /**
     * Appends a question link.
     *
     * @param question question record
     * @param position 1-based position in the quiz
     */
    public void addQuestion(QuestionRecord question, int position) {
        questions.add(new QuizQuestionRecord(this, question, position));
    }
}
