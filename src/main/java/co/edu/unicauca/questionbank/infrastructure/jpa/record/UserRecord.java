package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.*;

/** JPA representation of a user, mapped to the {@code users} table. */
@Entity
@Table(name = "users")
public class UserRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoleColumn role;

    /** Constructor required by JPA. */
    protected UserRecord() { }

    /**
     * Creates a UserRecord with the given values.
     *
     * @param fullName full name
     * @param email    unique e-mail
     * @param role     role
     */
    public UserRecord(String fullName, String email, RoleColumn role) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    /**
     * Returns primary key.
     *
     * @return primary key
     */
    public Long getId() { return id; }
    /**
     * Returns full name.
     *
     * @return full name
     */
    public String getFullName() { return fullName; }
    /**
     * Returns e-mail.
     *
     * @return e-mail
     */
    public String getEmail() { return email; }
    /**
     * Returns role.
     *
     * @return role
     */
    public RoleColumn getRole() { return role; }
    /**
     * Sets the new full name.
     *
     * @param fullName new full name
     */
    public void setFullName(String fullName) { this.fullName = fullName; }
    /**
     * Sets the new e-mail.
     *
     * @param email new e-mail
     */
    public void setEmail(String email) { this.email = email; }
    /**
     * Sets the new role.
     *
     * @param role new role
     */
    public void setRole(RoleColumn role) { this.role = role; }
}
