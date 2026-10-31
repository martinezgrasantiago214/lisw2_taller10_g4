package co.edu.unicauca.questionbank.infrastructure.jpa.mapper;

import co.edu.unicauca.questionbank.domain.model.*;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single translation point between domain objects and JPA records, in both directions.
 * The domain never sees a record and records never contain business rules.
 */
public final class DomainMapper {
    private DomainMapper() { }

    // ---------------------------------------------------------------- record to domain

    /**
     * Returns domain user.
     *
     * @param r user record
     * @return domain user
     */
    public static User toDomain(UserRecord r) {
        return new User(r.getId(), r.getFullName(), r.getEmail(), Role.valueOf(r.getRole().name()));
    }

    /**
     * Returns domain topic.
     *
     * @param r topic record
     * @return domain topic
     */
    public static Topic toDomain(TopicRecord r) { return new Topic(r.getId(), r.getName(), r.getModule()); }

    /**
     * Returns domain competency.
     *
     * @param r competency record
     * @return domain competency
     */
    public static Competency toDomain(CompetencyRecord r) { return new Competency(r.getId(), r.getCode(), r.getName()); }

    /**
     * Rebuilds the domain subtype that matches the record subtype.
     *
     * @param r question record of any subtype
     * @return domain question
     * @throws IllegalStateException if the record subtype is unknown
     */
    public static Question toDomain(QuestionRecord r) {
        DifficultyLevel difficulty = DifficultyLevel.valueOf(r.getDifficulty().name());
        QuestionStatus status = QuestionStatus.valueOf(r.getStatus().name());
        Long authorId = r.getAuthor().getId();
        Long topicId = r.getTopic().getId();

        Question question;
        if (r instanceof MultipleChoiceQuestionRecord mc) {
            MultipleChoiceQuestion q = new MultipleChoiceQuestion(r.getId(), r.getStatement(), difficulty, status, authorId, topicId);
            mc.getOptions().forEach(o -> q.restoreOption(new AnswerOption(o.getId(), o.getLabel(), o.getText(), o.isCorrect())));
            question = q;
        } else if (r instanceof TrueFalseQuestionRecord tf) {
            question = new TrueFalseQuestion(r.getId(), r.getStatement(), difficulty, status, authorId, topicId, tf.getExpectedValue());
        } else if (r instanceof OpenQuestionRecord open) {
            question = new OpenQuestion(r.getId(), r.getStatement(), difficulty, status, authorId, topicId,
                open.getExpectedAnswer(), open.getMaxWords());
        } else {
            throw new IllegalStateException("Unsupported question record: " + r.getClass().getName());
        }
        r.getCompetencies().forEach(c -> question.restoreCompetency(c.getId()));
        QuestionMetadataRecord m = r.getMetadata();
        if (m != null) {
            question.setMetadata(new QuestionMetadata(m.getLastReviewDate(), m.getCognitiveLevel(),
                m.getEstimatedTimeMinutes(), m.getEditorialObservations()));
        }
        return question;
    }

    /**
     * Returns domain quiz with its questions in stored order.
     *
     * @param r quiz record
     * @return domain quiz with its questions in stored order
     */
    public static Quiz toDomain(QuizRecord r) {
        List<Long> questionIds = r.getQuestions().stream()
            .sorted(Comparator.comparingInt(QuizQuestionRecord::getPosition))
            .map(link -> link.getQuestion().getId())
            .toList();
        return new Quiz(r.getId(), r.getTitle(), r.getDescription(), questionIds);
    }

    // ---------------------------------------------------------------- domain to record

    /**
     * Returns stored role value.
     *
     * @param role domain role
     * @return stored role value
     */
    public static RoleColumn toColumn(Role role) { return RoleColumn.valueOf(role.name()); }

    /**
     * Returns stored state value.
     *
     * @param status domain state
     * @return stored state value
     */
    public static StatusColumn toColumn(QuestionStatus status) { return StatusColumn.valueOf(status.name()); }

    /**
     * Creates an empty record of the subtype that matches the domain question.
     *
     * @param q domain question
     * @return new empty record
     */
    public static QuestionRecord newRecordFor(Question q) {
        if (q instanceof MultipleChoiceQuestion) return new MultipleChoiceQuestionRecord();
        if (q instanceof TrueFalseQuestion) return new TrueFalseQuestionRecord();
        if (q instanceof OpenQuestion) return new OpenQuestionRecord();
        throw new IllegalArgumentException("Unsupported question type: " + q.getClass().getName());
    }

    /**
     * Copies the state of a domain question into a record, for both creation and update.
     *
     * @param q            domain question
     * @param r            target record of the matching subtype
     * @param author       managed author record
     * @param topic        managed topic record
     * @param competencies managed competency records
     * @throws IllegalArgumentException if the record subtype does not match the question type
     */
    public static void copyToRecord(Question q, QuestionRecord r, UserRecord author, TopicRecord topic,
                                    Set<CompetencyRecord> competencies) {
        r.setStatement(q.getStatement());
        r.setDifficulty(DifficultyColumn.valueOf(q.getDifficulty().name()));
        r.setStatus(toColumn(q.getStatus()));
        r.setAuthor(author);
        r.setTopic(topic);
        r.replaceCompetencies(competencies);
        copyMetadata(q.getMetadata(), r);

        if (q instanceof MultipleChoiceQuestion mc && r instanceof MultipleChoiceQuestionRecord mcr) {
            if (!sameOptions(mc, mcr)) {
                mcr.clearOptions();
                mc.getOptions().forEach(o -> mcr.addOption(new AnswerOptionRecord(o.getLabel(), o.getText(), o.isCorrect())));
            }
        } else if (q instanceof TrueFalseQuestion tf && r instanceof TrueFalseQuestionRecord tfr) {
            tfr.setExpectedValue(tf.getExpectedValue());
        } else if (q instanceof OpenQuestion open && r instanceof OpenQuestionRecord or) {
            or.setExpectedAnswer(open.getExpectedAnswer());
            or.setMaxWords(open.getMaxWords());
        } else {
            throw new IllegalArgumentException("The type of an existing question cannot change");
        }
    }

    private static void copyMetadata(QuestionMetadata source, QuestionRecord target) {
        if (source == null) {
            target.setMetadata(null);
            return;
        }
        QuestionMetadataRecord m = target.getMetadata() != null ? target.getMetadata() : new QuestionMetadataRecord();
        m.setLastReviewDate(source.getLastReviewDate());
        m.setCognitiveLevel(source.getCognitiveLevel());
        m.setEstimatedTimeMinutes(source.getEstimatedTimeMinutes());
        m.setEditorialObservations(source.getEditorialObservations());
        target.setMetadata(m);
    }

    /** Avoids deleting and re-inserting options when they did not change (for example, on a state change). */
    private static boolean sameOptions(MultipleChoiceQuestion domain, MultipleChoiceQuestionRecord stored) {
        Set<String> wanted = domain.getOptions().stream()
            .map(o -> o.getLabel() + "|" + o.getText() + "|" + o.isCorrect()).collect(Collectors.toSet());
        Set<String> current = stored.getOptions().stream()
            .map(o -> o.getLabel() + "|" + o.getText() + "|" + o.isCorrect()).collect(Collectors.toSet());
        return wanted.equals(current);
    }
}
