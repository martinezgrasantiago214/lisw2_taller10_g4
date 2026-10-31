package co.edu.unicauca.questionbank.infrastructure.jpa.record;

/** Stored values for the question lifecycle state. */
public enum StatusColumn {
    /** Draft. */ DRAFT,
    /** Waiting for review. */ PENDING_REVIEW,
    /** Under review. */ IN_REVIEW,
    /** Approved. */ APPROVED,
    /** Published. */ PUBLISHED,
    /** Archived. */ ARCHIVED,
    /** Rejected. */ REJECTED
}
