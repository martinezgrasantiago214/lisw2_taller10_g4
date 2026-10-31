package co.edu.unicauca.questionbank.domain.model;

/**
 * Lifecycle states of a question:
 * DRAFT, PENDING_REVIEW, IN_REVIEW, APPROVED, PUBLISHED, ARCHIVED, plus REJECTED,
 * from which the question returns to DRAFT for correction.
 */
public enum QuestionStatus {
    /** Being written or corrected by its author. */
    DRAFT,
    /** Submitted and waiting for a reviewer. */
    PENDING_REVIEW,
    /** A reviewer is evaluating it. */
    IN_REVIEW,
    /** Accepted by the reviewer but not yet available for quizzes. */
    APPROVED,
    /** Available for quizzes and mock exams. */
    PUBLISHED,
    /** Withdrawn from use. */
    ARCHIVED,
    /** Returned by the reviewer; the author must correct it. */
    REJECTED
}
