package co.edu.unicauca.questionbank.domain.model;

/** Binary question whose expected answer is either true or false. */
public class TrueFalseQuestion extends Question {
    /** Type name exposed to clients. */
    public static final String TYPE = "TRUE_FALSE";

    private final boolean expectedValue;

    /**
     * Rebuilds a true/false question.
     *
     * @param id            persistent identifier
     * @param statement     statement
     * @param difficulty    difficulty level
     * @param status        lifecycle state
     * @param authorId      author identifier
     * @param topicId       topic identifier
     * @param expectedValue expected truth value
     */
    public TrueFalseQuestion(Long id, String statement, DifficultyLevel difficulty, QuestionStatus status,
                             Long authorId, Long topicId, boolean expectedValue) {
        super(id, statement, difficulty, status, authorId, topicId);
        this.expectedValue = expectedValue;
    }

    /**
     * Creates a new draft true/false question.
     *
     * @param statement     statement
     * @param difficulty    difficulty level
     * @param authorId      author identifier
     * @param topicId       topic identifier
     * @param expectedValue expected truth value
     */
    public TrueFalseQuestion(String statement, DifficultyLevel difficulty, Long authorId, Long topicId,
                             boolean expectedValue) {
        this(null, statement, difficulty, QuestionStatus.DRAFT, authorId, topicId, expectedValue);
    }

    /**
     * Returns expected truth value.
     *
     * @return expected truth value
     */
    public boolean getExpectedValue() { return expectedValue; }

    /** {@inheritDoc} */
    @Override public String getType() { return TYPE; }

    /**
     * {@inheritDoc}
     *
     * @return always {@code true}: the statement and the expected value are already mandatory
     */
    @Override public boolean isReadyForReview() { return true; }
}
