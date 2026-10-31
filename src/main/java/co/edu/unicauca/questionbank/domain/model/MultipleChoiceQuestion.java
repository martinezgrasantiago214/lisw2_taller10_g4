package co.edu.unicauca.questionbank.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Single-answer multiple-choice question. Following the academic rules of the system, a
 * complete item has exactly five options: one correct answer and four distractors.
 */
public class MultipleChoiceQuestion extends Question {
    /** Number of options of a complete item. */
    public static final int REQUIRED_OPTIONS = 5;
    /** Type name exposed to clients. */
    public static final String TYPE = "MULTIPLE_CHOICE";

    private final List<AnswerOption> options = new ArrayList<>();

    /**
     * Rebuilds a multiple-choice question.
     *
     * @param id         persistent identifier
     * @param statement  statement
     * @param difficulty difficulty level
     * @param status     lifecycle state
     * @param authorId   author identifier
     * @param topicId    topic identifier
     */
    public MultipleChoiceQuestion(Long id, String statement, DifficultyLevel difficulty, QuestionStatus status,
                                  Long authorId, Long topicId) {
        super(id, statement, difficulty, status, authorId, topicId);
    }

    /**
     * Creates a new draft multiple-choice question.
     *
     * @param statement  statement
     * @param difficulty difficulty level
     * @param authorId   author identifier
     * @param topicId    topic identifier
     */
    public MultipleChoiceQuestion(String statement, DifficultyLevel difficulty, Long authorId, Long topicId) {
        this(null, statement, difficulty, QuestionStatus.DRAFT, authorId, topicId);
    }

    /**
     * Adds an option to an editable question.
     *
     * @param option option to add
     * @throws IllegalArgumentException if the label is repeated
     * @throws IllegalStateException    if the question already has five options or cannot be edited
     */
    public void addOption(AnswerOption option) {
        ensureEditable();
        addOptionInternal(option);
    }

    /**
     * Restores an option while rebuilding a stored question (no edit check).
     *
     * @param option stored option
     */
    public void restoreOption(AnswerOption option) { addOptionInternal(option); }

    /**
     * Replaces every option of an editable question.
     *
     * @param newOptions new options
     */
    public void replaceOptions(List<AnswerOption> newOptions) {
        ensureEditable();
        options.clear();
        newOptions.forEach(this::addOptionInternal);
    }

    private void addOptionInternal(AnswerOption option) {
        if (option == null) throw new IllegalArgumentException("Option is required");
        boolean duplicated = options.stream().anyMatch(o -> o.getLabel().equals(option.getLabel()));
        if (duplicated) throw new IllegalArgumentException("Duplicated option label: " + option.getLabel());
        if (options.size() >= REQUIRED_OPTIONS)
            throw new IllegalStateException("A question cannot have more than " + REQUIRED_OPTIONS + " options");
        options.add(option);
    }

    /**
     * Returns copy of the options in insertion order.
     *
     * @return copy of the options in insertion order
     */
    public List<AnswerOption> getOptions() { return List.copyOf(options); }

    /**
     * Finds the correct option. It is not named {@code getX} so JSON serializers ignore it.
     *
     * @return the correct option, if any
     */
    public Optional<AnswerOption> findCorrectOption() {
        return options.stream().filter(AnswerOption::isCorrect).findFirst();
    }

    /** {@inheritDoc} */
    @Override public String getType() { return TYPE; }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if there are exactly five options and exactly one is correct
     */
    @Override public boolean isReadyForReview() {
        long correct = options.stream().filter(AnswerOption::isCorrect).count();
        return options.size() == REQUIRED_OPTIONS && correct == 1;
    }
}
