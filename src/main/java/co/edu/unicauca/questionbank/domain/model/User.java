package co.edu.unicauca.questionbank.domain.model;

/**
 * A person who interacts with the question bank. Pure domain class without persistence annotations.
 */
public class User {
    private Long id;
    private final String fullName;
    private final String email;
    private final Role role;

    /**
     * Creates a user.
     *
     * @param id       persistent identifier, or {@code null} for a new user
     * @param fullName full name, required
     * @param email    e-mail address, required
     * @param role     role in the system, required
     * @throws IllegalArgumentException if a required value is missing or invalid
     */
    public User(Long id, String fullName, String email, Role role) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("User full name is required");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("Invalid user e-mail: " + email);
        if (role == null) throw new IllegalArgumentException("User role is required");
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    /**
     * Tells whether this user may be registered as the author of a question.
     *
     * @return {@code true} if the user has the {@link Role#AUTHOR} role
     */
    public boolean canAuthorQuestions() { return role == Role.AUTHOR; }

    /**
     * Returns persistent identifier, or {@code null} if not saved yet.
     *
     * @return persistent identifier, or {@code null} if not saved yet
     */
    public Long getId() { return id; }

    /**
     * Sets the persistent identifier assigned by the repository.
     *
     * @param id persistent identifier assigned by the repository
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns full name.
     *
     * @return full name
     */
    public String getFullName() { return fullName; }

    /**
     * Returns e-mail address.
     *
     * @return e-mail address
     */
    public String getEmail() { return email; }

    /**
     * Returns role in the system.
     *
     * @return role in the system
     */
    public Role getRole() { return role; }
}
