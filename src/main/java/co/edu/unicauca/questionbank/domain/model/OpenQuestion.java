package co.edu.unicauca.questionbank.domain.model;

/** Open-ended question, for example in the written communication module. */
public class OpenQuestion extends Question {
    /** Type name exposed to clients. */
    public static final String TYPE = "OPEN";

    private String expectedAnswer;
    private int maxWords;

    /**
     * Rebuilds an open-ended question.
     *
     * @param id             persistent identifier
     * @param statement      statement
     * @param difficulty     difficulty level
     * @param status         lifecycle state
     * @param authorId       author identifier
     * @param topicId        topic identifier
     * @param expectedAnswer grading guidelines
     * @param maxWords       maximum number of words, not negative
     */
    public OpenQuestion(Long id, String statement, DifficultyLevel difficulty, QuestionStatus status,
                        Long authorId, Long topicId, String expectedAnswer, int maxWords) {
        super(id, statement, difficulty, status, authorId, topicId);
        if (maxWords < 0) throw new IllegalArgumentException("Maximum words cannot be negative");
        this.expectedAnswer = expectedAnswer;
        this.maxWords = maxWords;
    }

    /**
     * Creates a new draft open-ended question.
     *
     * @param statement      statement
     * @param difficulty     difficulty level
     * @param authorId       author identifier
     * @param topicId        topic identifier
     * @param expectedAnswer grading guidelines
     * @param maxWords       maximum number of words
     */
    public OpenQuestion(String statement, DifficultyLevel difficulty, Long authorId, Long topicId,
                        String expectedAnswer, int maxWords) {
        this(null, statement, difficulty, QuestionStatus.DRAFT, authorId, topicId, expectedAnswer, maxWords);
    }

    /**
     * Changes the grading guidelines of an editable question.
     *
     * @param newExpectedAnswer grading guidelines
     * @param newMaxWords       maximum number of words
     */
    public void changeGuidelines(String newExpectedAnswer, int newMaxWords) {
        ensureEditable();
        if (newMaxWords < 0) throw new IllegalArgumentException("Maximum words cannot be negative");
        this.expectedAnswer = newExpectedAnswer;
        this.maxWords = newMaxWords;
    }

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

    /** {@inheritDoc} */
    @Override public String getType() { return TYPE; }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if grading guidelines and a positive word limit are defined
     */
    @Override public boolean isReadyForReview() {
        return expectedAnswer != null && !expectedAnswer.isBlank() && maxWords > 0;
    }
}
