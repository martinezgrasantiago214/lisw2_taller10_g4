package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.User;
import java.util.List;
import java.util.Optional;

/** Technology-independent persistence contract for {@link User}. */
public interface UserRepository {
    /**
     * Creates or updates a user.
     *
     * @param user user to store
     * @return the stored user
     */
    User save(User user);

    /**
     * Returns the user, if it exists.
     *
     * @param id identifier
     * @return the user, if it exists
     */
    Optional<User> findById(Long id);

    /**
     * Returns every user.
     *
     * @return every user
     */
    List<User> findAll();

    /**
     * Returns {@code true} if the user exists.
     *
     * @param id identifier
     * @return {@code true} if the user exists
     */
    boolean existsById(Long id);
}
