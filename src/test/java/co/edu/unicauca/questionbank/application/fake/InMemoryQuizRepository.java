package co.edu.unicauca.questionbank.application.fake;

import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.repository.QuizRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory implementation of {@link QuizRepository} for technology-free tests. */
public class InMemoryQuizRepository implements QuizRepository {
    private final Map<Long, Quiz> store = new LinkedHashMap<>();
    private long sequence = 0;

    /** {@inheritDoc} */
    @Override public Quiz save(Quiz quiz) {
        Quiz stored = quiz.getId() != null ? quiz : new Quiz(++sequence, quiz.getTitle(), quiz.getDescription(), quiz.getQuestionIds());
        store.put(stored.getId(), stored);
        return stored;
    }
    /** {@inheritDoc} */
    @Override public Optional<Quiz> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    /** {@inheritDoc} */
    @Override public List<Quiz> findAll() { return new ArrayList<>(store.values()); }
}
