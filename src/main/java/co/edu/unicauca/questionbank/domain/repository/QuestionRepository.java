package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import java.util.List;
import java.util.Optional;

/**
 * Technology-independent persistence contract for questions of every type.
 * It uses only domain types: no Spring, JPA or SQL concepts.
 */
public interface QuestionRepository {
    /**
     * Creates or updates a question.
     *
     * @param question question to store
     * @return the stored question, with generated identifiers
     */
    Question save(Question question);

    /**
     * Returns the question, if it exists.
     *
     * @param id question identifier
     * @return the question, if it exists
     */
    Optional<Question> findById(Long id);

    /**
     * Returns every question, whatever its type.
     *
     * @return every question, whatever its type
     */
    List<Question> findAll();

    /**
     * Returns questions written by that author.
     *
     * @param authorId author identifier
     * @return questions written by that author
     */
    List<Question> findByAuthorId(Long authorId);

    /**
     * Returns questions in that state.
     *
     * @param status lifecycle state
     * @return questions in that state
     */
    List<Question> findByStatus(QuestionStatus status);

    /**
     * Returns questions of that topic.
     *
     * @param topicId topic identifier
     * @return questions of that topic
     */
    List<Question> findByTopicId(Long topicId);

    /**
     * Returns published questions that assess that competency.
     *
     * @param competencyId competency identifier
     * @return published questions that assess that competency
     */
    List<Question> findPublishedByCompetencyId(Long competencyId);

    /**
     * Returns {@code true} if the question exists.
     *
     * @param id question identifier
     * @return {@code true} if the question exists
     */
    boolean existsById(Long id);

    /**
     * Delete by id.
     *
     * @param id identifier of the question to delete, together with its options and metadata
     */
    void deleteById(Long id);
}
