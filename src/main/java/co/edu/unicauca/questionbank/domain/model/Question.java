package co.edu.unicauca.questionbank.domain.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Common business concept of a question in the Saber PRO bank.
 *
 * <p>It owns the lifecycle rules and the validations shared by every question type and it
 * does not depend on any persistence technology. Associations with other aggregates
 * (author, topic, competencies) are kept by identifier; answer options and metadata are
 * part of the question itself.</p>
 */
public abstract class Question {
    private Long id;
    private String statement;
    private final DifficultyLevel difficulty;
    private QuestionStatus status;
    private final Long authorId;
    private Long topicId;
    private final Set<Long> competencyIds = new LinkedHashSet<>();
    private QuestionMetadata metadata;

    /**
     * Initializes the common state of a question.
     *
     * @param id         persistent identifier, or {@code null} for a new question
     * @param statement  question statement, required
     * @param difficulty difficulty level, required
     * @param status     lifecycle state; {@code null} means {@link QuestionStatus#DRAFT}
     * @param authorId   identifier of the authoring user, required
     * @param topicId    identifier of the topic, required
     * @throws IllegalArgumentException if a required value is missing
     */
    protected Question(Long id, String statement, DifficultyLevel difficulty, QuestionStatus status,
                       Long authorId, Long topicId) {
        if (statement == null || statement.isBlank()) throw new IllegalArgumentException("Statement is required");
        if (difficulty == null) throw new IllegalArgumentException("Difficulty is required");
        if (authorId == null) throw new IllegalArgumentException("A question must have an author");
        if (topicId == null) throw new IllegalArgumentException("A question must have a topic");
        this.id = id;
        this.statement = statement;
        this.difficulty = difficulty;
        this.status = status == null ? QuestionStatus.DRAFT : status;
        this.authorId = authorId;
        this.topicId = topicId;
    }

    /**
     * Returns the question type, used by REST clients to tell the subtypes apart.
     *
     * @return type name, for example {@code MULTIPLE_CHOICE}
     */
    public abstract String getType();

    /**
     * Type-specific rule that tells whether the content is complete enough to be reviewed.
     *
     * @return {@code true} if the question meets the minimum content requirements
     */
    public abstract boolean isReadyForReview();

    /**
     * Sends a complete draft to the review queue (DRAFT to PENDING_REVIEW).
     *
     * @throws IllegalStateException if the question is not a draft, is incomplete or has no competency
     */
    public void submitForReview() {
        requireStatus(QuestionStatus.DRAFT, "Only draft questions can be submitted for review");
        if (!isReadyForReview())
            throw new IllegalStateException("The question does not meet the minimum review requirements");
        if (competencyIds.isEmpty())
            throw new IllegalStateException("A question must assess at least one competency before review");
        status = QuestionStatus.PENDING_REVIEW;
    }

    /**
     * A reviewer takes a pending question (PENDING_REVIEW to IN_REVIEW).
     *
     * @throws IllegalStateException if the question is not pending review
     */
    public void startReview() {
        requireStatus(QuestionStatus.PENDING_REVIEW, "Only pending questions can enter review");
        status = QuestionStatus.IN_REVIEW;
    }

    /**
     * Approves a question under review (IN_REVIEW to APPROVED).
     *
     * @throws IllegalStateException if the question is not under review
     */
    public void approve() {
        requireStatus(QuestionStatus.IN_REVIEW, "Only questions in review can be approved");
        status = QuestionStatus.APPROVED;
    }

    /**
     * Rejects a question under review (IN_REVIEW to REJECTED).
     *
     * @throws IllegalStateException if the question is not under review
     */
    public void reject() {
        requireStatus(QuestionStatus.IN_REVIEW, "Only questions in review can be rejected");
        status = QuestionStatus.REJECTED;
    }

    /**
     * Returns a rejected question to its author for correction (REJECTED to DRAFT).
     *
     * @throws IllegalStateException if the question is not rejected
     */
    public void returnToDraft() {
        requireStatus(QuestionStatus.REJECTED, "Only rejected questions can return to draft");
        status = QuestionStatus.DRAFT;
    }

    /**
     * Publishes an approved question so it can be used in quizzes (APPROVED to PUBLISHED).
     *
     * @throws IllegalStateException if the question is not approved
     */
    public void publish() {
        requireStatus(QuestionStatus.APPROVED, "Only approved questions can be published");
        status = QuestionStatus.PUBLISHED;
    }

    /**
     * Withdraws a published question from use (PUBLISHED to ARCHIVED).
     *
     * @throws IllegalStateException if the question is not published
     */
    public void archive() {
        requireStatus(QuestionStatus.PUBLISHED, "Only published questions can be archived");
        status = QuestionStatus.ARCHIVED;
    }

    /**
     * Tells whether the question may be included in a quiz.
     *
     * @return {@code true} only for published questions
     */
    public boolean isUsableInQuizzes() { return status == QuestionStatus.PUBLISHED; }

    /**
     * Classifies the question: assigns its topic and replaces its competencies.
     *
     * @param newTopicId       topic identifier, required
     * @param newCompetencyIds competency identifiers; at least one is required
     * @throws IllegalArgumentException if the topic or the competencies are missing
     * @throws IllegalStateException    if the question cannot be edited
     */
    public void classify(Long newTopicId, Collection<Long> newCompetencyIds) {
        ensureEditable();
        if (newTopicId == null) throw new IllegalArgumentException("A question must have a topic");
        if (newCompetencyIds == null || newCompetencyIds.isEmpty())
            throw new IllegalArgumentException("At least one competency is required");
        for (Long competencyId : newCompetencyIds) {
            if (competencyId == null) throw new IllegalArgumentException("Invalid competency");
        }
        topicId = newTopicId;
        competencyIds.clear();
        competencyIds.addAll(newCompetencyIds);
    }

    /**
     * Adds one competency to the question.
     *
     * @param competencyId competency identifier
     * @throws IllegalArgumentException if the identifier is {@code null}
     * @throws IllegalStateException    if the question cannot be edited
     */
    public void addCompetency(Long competencyId) {
        ensureEditable();
        restoreCompetency(competencyId);
    }

    /**
     * Restores a competency identifier while rebuilding a stored question; it skips the
     * edit check because stored questions may already be published.
     *
     * @param competencyId competency identifier
     */
    public void restoreCompetency(Long competencyId) {
        if (competencyId == null) throw new IllegalArgumentException("Invalid competency");
        competencyIds.add(competencyId);
    }

    /**
     * Changes the statement of an editable question.
     *
     * @param newStatement new statement, required
     */
    public void changeStatement(String newStatement) {
        ensureEditable();
        if (newStatement == null || newStatement.isBlank()) throw new IllegalArgumentException("Statement is required");
        this.statement = newStatement;
    }

    /**
     * Guards content changes: only drafts and rejected questions can be edited.
     *
     * @throws IllegalStateException otherwise
     */
    protected void ensureEditable() {
        if (status != QuestionStatus.DRAFT && status != QuestionStatus.REJECTED)
            throw new IllegalStateException("Only draft or rejected questions can be edited");
    }

    private void requireStatus(QuestionStatus expected, String message) {
        if (status != expected) throw new IllegalStateException(message);
    }

    /**
     * Returns persistent identifier, or {@code null}.
     *
     * @return persistent identifier, or {@code null}
     */
    public Long getId() { return id; }

    /**
     * Sets the persistent identifier assigned by the repository.
     *
     * @param id persistent identifier assigned by the repository
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns statement.
     *
     * @return statement
     */
    public String getStatement() { return statement; }

    /**
     * Returns difficulty level.
     *
     * @return difficulty level
     */
    public DifficultyLevel getDifficulty() { return difficulty; }

    /**
     * Returns current lifecycle state.
     *
     * @return current lifecycle state
     */
    public QuestionStatus getStatus() { return status; }

    /**
     * Returns identifier of the authoring user.
     *
     * @return identifier of the authoring user
     */
    public Long getAuthorId() { return authorId; }

    /**
     * Returns identifier of the topic.
     *
     * @return identifier of the topic
     */
    public Long getTopicId() { return topicId; }

    /**
     * Returns read-only view of the competency identifiers.
     *
     * @return read-only view of the competency identifiers
     */
    public Set<Long> getCompetencyIds() { return Collections.unmodifiableSet(competencyIds); }

    /**
     * Returns optional metadata, or {@code null}.
     *
     * @return optional metadata, or {@code null}
     */
    public QuestionMetadata getMetadata() { return metadata; }

    /**
     * Sets the optional metadata; {@code null} removes it.
     *
     * @param metadata optional metadata; {@code null} removes it
     */
    public void setMetadata(QuestionMetadata metadata) { this.metadata = metadata; }
}
