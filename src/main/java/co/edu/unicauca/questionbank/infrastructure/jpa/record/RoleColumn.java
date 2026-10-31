package co.edu.unicauca.questionbank.infrastructure.jpa.record;

/**
 * Stored values for user roles. Kept apart from the domain enum so that renaming a domain
 * constant never silently changes stored data.
 */
public enum RoleColumn {
    /** Administrator. */ ADMIN,
    /** Question author. */ AUTHOR,
    /** Question reviewer. */ REVIEWER,
    /** Teacher who builds quizzes. */ TEACHER,
    /** Student. */ STUDENT
}
