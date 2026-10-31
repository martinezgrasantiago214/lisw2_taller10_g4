package co.edu.unicauca.questionbank.application.fake;

import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of {@link QuestionRepository} used to test use cases without JPA,
 * Spring or a database. It shows that the contract is independent of the technology.
 */
public class InMemoryQuestionRepository implements QuestionRepository {
    private final Map<Long, Question> store = new LinkedHashMap<>();
    private long sequence = 0;

    /** {@inheritDoc} */
    @Override public Question save(Question question) {
        if (question.getId() == null) question.setId(++sequence);
        store.put(question.getId(), question);
        return question;
    }
    /** {@inheritDoc} */
    @Override public Optional<Question> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    /** {@inheritDoc} */
    @Override public List<Question> findAll() { return new ArrayList<>(store.values()); }
    /** {@inheritDoc} */
    @Override public List<Question> findByAuthorId(Long authorId) {
        return store.values().stream().filter(q -> q.getAuthorId().equals(authorId)).toList();
    }
    /** {@inheritDoc} */
    @Override public List<Question> findByStatus(QuestionStatus status) {
        return store.values().stream().filter(q -> q.getStatus() == status).toList();
    }
    /** {@inheritDoc} */
    @Override public List<Question> findByTopicId(Long topicId) {
        return store.values().stream().filter(q -> q.getTopicId().equals(topicId)).toList();
    }
    /** {@inheritDoc} */
    @Override public List<Question> findPublishedByCompetencyId(Long competencyId) {
        return store.values().stream()
            .filter(q -> q.getStatus() == QuestionStatus.PUBLISHED && q.getCompetencyIds().contains(competencyId)).toList();
    }
    /** {@inheritDoc} */
    @Override public boolean existsById(Long id) { return store.containsKey(id); }
    /** {@inheritDoc} */
    @Override public void deleteById(Long id) { store.remove(id); }
}
