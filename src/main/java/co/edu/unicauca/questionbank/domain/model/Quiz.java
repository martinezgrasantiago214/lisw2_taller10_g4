package co.edu.unicauca.questionbank.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quiz (mock exam) composed of an ordered list of questions. Only published questions can be
 * added; the application layer checks that rule because it needs to load each question.
 */
public final class Quiz {
    private Long id;
    private final String title;
    private final String description;
    private final List<Long> questionIds = new ArrayList<>();

    /**
     * Creates or rebuilds a quiz.
     *
     * @param id          persistent identifier, or {@code null}
     * @param title       title, required
     * @param description optional description
     * @param questionIds ordered question identifiers
     * @throws IllegalArgumentException if the title is missing or a question is repeated
     */
    public Quiz(Long id, String title, String description, List<Long> questionIds) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Quiz title is required");
        this.id = id;
        this.title = title.trim();
        this.description = description;
        if (questionIds != null) questionIds.forEach(this::addQuestion);
    }

    /**
     * Creates a new quiz.
     *
     * @param title       title
     * @param description description
     * @param questionIds ordered question identifiers
     */
    public Quiz(String title, String description, List<Long> questionIds) {
        this(null, title, description, questionIds);
    }

    /**
     * Appends a question at the end of the quiz.
     *
     * @param questionId question identifier
     * @throws IllegalArgumentException if it is {@code null} or already included
     */
    public void addQuestion(Long questionId) {
        if (questionId == null) throw new IllegalArgumentException("Question id is required");
        if (questionIds.contains(questionId)) throw new IllegalArgumentException("Question already belongs to this quiz: " + questionId);
        questionIds.add(questionId);
    }

    /**
     * Returns persistent identifier.
     *
     * @return persistent identifier
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
     * Returns read-only ordered question identifiers.
     *
     * @return read-only ordered question identifiers
     */
    public List<Long> getQuestionIds() { return Collections.unmodifiableList(questionIds); }
}
