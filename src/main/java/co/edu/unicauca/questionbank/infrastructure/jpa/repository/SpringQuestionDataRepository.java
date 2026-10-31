package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.StatusColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data interface used only inside the infrastructure layer. Queries are polymorphic:
 * each result is the record subtype that matches the stored row.
 */
public interface SpringQuestionDataRepository extends JpaRepository<QuestionRecord, Long> {
    /**
     * Returns questions whose {@code author_id} matches.
     *
     * @param authorId author identifier
     * @return questions whose {@code author_id} matches
     */
    List<QuestionRecord> findByAuthor_Id(Long authorId);

    /**
     * Returns questions in that state.
     *
     * @param status lifecycle state
     * @return questions in that state
     */
    List<QuestionRecord> findByStatus(StatusColumn status);

    /**
     * Returns questions whose {@code topic_id} matches.
     *
     * @param topicId topic identifier
     * @return questions whose {@code topic_id} matches
     */
    List<QuestionRecord> findByTopic_Id(Long topicId);

    /**
     * Derived query that joins {@code question_competencies} and filters by state.
     *
     * @param status       lifecycle state
     * @param competencyId competency identifier
     * @return matching questions
     */
    List<QuestionRecord> findByStatusAndCompetencies_Id(StatusColumn status, Long competencyId);
}
