package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Topic;
import java.util.List;
import java.util.Optional;

/** Technology-independent persistence contract for {@link Topic}. */
public interface TopicRepository {
    /**
     * Creates or updates a topic.
     *
     * @param topic topic to store
     * @return the stored topic
     */
    Topic save(Topic topic);

    /**
     * Returns the topic, if it exists.
     *
     * @param id identifier
     * @return the topic, if it exists
     */
    Optional<Topic> findById(Long id);

    /**
     * Returns every topic.
     *
     * @return every topic
     */
    List<Topic> findAll();

    /**
     * Returns {@code true} if the topic exists.
     *
     * @param id identifier
     * @return {@code true} if the topic exists
     */
    boolean existsById(Long id);
}
