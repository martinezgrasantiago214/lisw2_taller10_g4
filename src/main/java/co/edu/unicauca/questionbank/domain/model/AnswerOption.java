package co.edu.unicauca.questionbank.domain.model;

/** Immutable answer option (A to E) of a multiple-choice question. */
public class AnswerOption {
    private final Long id;
    private final String label;
    private final String text;
    private final boolean correct;

    /**
     * Creates an answer option, typically when rebuilding it from storage.
     *
     * @param id      persistent identifier, or {@code null}
     * @param label   one-letter label; it is normalized to upper case
     * @param text    option text, required
     * @param correct whether this option is the correct answer
     * @throws IllegalArgumentException if the label or text is missing
     */
    public AnswerOption(Long id, String label, String text, boolean correct) {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Option label is required");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Option text is required");
        this.id = id;
        this.label = label.trim().toUpperCase();
        this.text = text;
        this.correct = correct;
    }

    /**
     * Creates a new option without a persistent identifier.
     *
     * @param label   one-letter label
     * @param text    option text
     * @param correct whether this option is the correct answer
     */
    public AnswerOption(String label, String text, boolean correct) { this(null, label, text, correct); }

    /**
     * Returns persistent identifier, or {@code null}.
     *
     * @return persistent identifier, or {@code null}
     */
    public Long getId() { return id; }

    /**
     * Returns upper-case label.
     *
     * @return upper-case label
     */
    public String getLabel() { return label; }

    /**
     * Returns option text.
     *
     * @return option text
     */
    public String getText() { return text; }

    /**
     * Returns {@code true} if this is the correct answer.
     *
     * @return {@code true} if this is the correct answer
     */
    public boolean isCorrect() { return correct; }
}
