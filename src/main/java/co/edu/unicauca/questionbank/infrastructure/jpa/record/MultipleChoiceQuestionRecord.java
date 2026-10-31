package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** JOINED subtype mapped to {@code multiple_choice_questions} (PK/FK {@code question_id}). */
@Entity
@Table(name = "multiple_choice_questions")
@PrimaryKeyJoinColumn(name = "question_id")
public class MultipleChoiceQuestionRecord extends QuestionRecord {

    /**
     * One-to-many, bidirectional: {@link AnswerOptionRecord#getQuestion()} owns the foreign key.
     * Cascade and orphan removal tie the options to the lifecycle of their question.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("label ASC")
    private List<AnswerOptionRecord> options = new ArrayList<>();

    /** Creates an empty record; the mapper fills it. */
    public MultipleChoiceQuestionRecord() { }

    /**
     * Returns managed list of options, ordered by label.
     *
     * @return managed list of options, ordered by label
     */
    public List<AnswerOptionRecord> getOptions() { return options; }

    /**
     * Adds an option and sets its owning side.
     *
     * @param option option record
     */
    public void addOption(AnswerOptionRecord option) {
        options.add(option);
        option.setQuestion(this);
    }

    /** Removes every option; orphan removal deletes the rows. */
    public void clearOptions() { options.clear(); }
}
