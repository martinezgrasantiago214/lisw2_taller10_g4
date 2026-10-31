package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Competency;
import java.util.List;
import java.util.Optional;

/** Technology-independent persistence contract for {@link Competency}. */
public interface CompetencyRepository {
    /**
     * Creates or updates a competency.
     *
     * @param competency competency to store
     * @return the stored competency
     */
    Competency save(Competency competency);

    /**
     * Returns the competency, if it exists.
     *
     * @param id identifier
     * @return the competency, if it exists
     */
    Optional<Competency> findById(Long id);

    /**
     * Returns every competency.
     *
     * @return every competency
     */
    List<Competency> findAll();

    /**
     * Returns {@code true} if the competency exists.
     *
     * @param id identifier
     * @return {@code true} if the competency exists
     */
    boolean existsById(Long id);
}
