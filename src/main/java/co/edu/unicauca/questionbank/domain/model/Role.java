package co.edu.unicauca.questionbank.domain.model;

/**
 * Roles that a user can play in the question bank, as described by the system actors.
 */
public enum Role {
    /** Manages users, roles and system parameters. */
    ADMIN,
    /** Creates and edits questions. */
    AUTHOR,
    /** Evaluates the quality and relevance of questions. */
    REVIEWER,
    /** Organizes quizzes and mock exams. */
    TEACHER,
    /** Answers questions and checks results. */
    STUDENT
}
