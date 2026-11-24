package co.unicauca.saberpro.question.testdoubles;

import co.unicauca.saberpro.question.access.IQuestionRepository;
import co.unicauca.saberpro.question.access.PagedResult;
import co.unicauca.saberpro.question.access.QuestionFilterCriteria;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble del puerto de persistencia de preguntas, para probar los casos de uso sin SQLite. */
public class InMemoryQuestionRepository implements IQuestionRepository {

    private final Map<String, Question> questions = new LinkedHashMap<>();
    private final List<QuestionVersion> versions = new ArrayList<>();

    @Override
    public boolean save(Question question) {
        return questions.putIfAbsent(question.getId(), question) == null;
    }

    @Override
    public boolean update(Question question) {
        return questions.replace(question.getId(), question) != null;
    }

    @Override
    public Optional<Question> findById(String id) {
        return Optional.ofNullable(questions.get(id));
    }

    @Override
    public PagedResult<Question> search(QuestionFilterCriteria criteria) {
        List<Question> all = questions.values().stream()
                .filter(q -> criteria.getAuthorLogin() == null || q.isAuthoredBy(criteria.getAuthorLogin()))
                .filter(q -> criteria.getStatus() == null || q.getStatus() == criteria.getStatus())
                .toList();
        return new PagedResult<>(all, 1, Math.max(all.size(), 1), all.size());
    }

    @Override
    public boolean saveVersion(QuestionVersion version) {
        return versions.add(version);
    }

    @Override
    public List<QuestionVersion> findVersions(String questionId) {
        return versions.stream().filter(v -> v.questionId().equals(questionId))
                .sorted(Comparator.comparingInt(QuestionVersion::version)).toList();
    }
}
