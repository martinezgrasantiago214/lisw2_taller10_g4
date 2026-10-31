package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.CompetencyRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.StatusColumn;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.TopicRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.UserRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringCompetencyDataRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringQuestionDataRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringTopicDataRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringUserDataRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA implementation of the {@link QuestionRepository} domain contract. It injects Spring Data
 * interfaces and uses {@link DomainMapper} to translate. Read methods run in a read-only
 * transaction so LAZY associations can be traversed while mapping.
 */
@Repository
@Transactional
public class QuestionRepositoryJpa implements QuestionRepository {
    private final SpringQuestionDataRepository questions;
    private final SpringUserDataRepository users;
    private final SpringTopicDataRepository topics;
    private final SpringCompetencyDataRepository competencies;

    /**
     * Creates the repository implementation with its injected dependencies.
     *
     * @param questions    Spring Data repository for questions
     * @param users        Spring Data repository for users
     * @param topics       Spring Data repository for topics
     * @param competencies Spring Data repository for competencies
     */
    public QuestionRepositoryJpa(SpringQuestionDataRepository questions, SpringUserDataRepository users,
                                 SpringTopicDataRepository topics, SpringCompetencyDataRepository competencies) {
        this.questions = questions;
        this.users = users;
        this.topics = topics;
        this.competencies = competencies;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the author, topic, a competency or the question itself does not exist
     */
    @Override
    public Question save(Question question) {
        UserRecord author = users.findById(question.getAuthorId())
            .orElseThrow(() -> new IllegalArgumentException("Author not found: " + question.getAuthorId()));
        TopicRecord topic = topics.findById(question.getTopicId())
            .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + question.getTopicId()));
        Set<CompetencyRecord> comps = new HashSet<>(competencies.findAllById(question.getCompetencyIds()));
        if (comps.size() != question.getCompetencyIds().size())
            throw new IllegalArgumentException("Competency not found among: " + question.getCompetencyIds());

        QuestionRecord record = question.getId() == null
            ? DomainMapper.newRecordFor(question)
            : questions.findById(question.getId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + question.getId()));

        DomainMapper.copyToRecord(question, record, author, topic, comps);
        // saveAndFlush runs the SQL now, so new options and metadata receive their identifiers
        return DomainMapper.toDomain(questions.saveAndFlush(record));
    }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public Optional<Question> findById(Long id) { return questions.findById(id).map(DomainMapper::toDomain); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Question> findAll() { return map(questions.findAll()); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Question> findByAuthorId(Long authorId) { return map(questions.findByAuthor_Id(authorId)); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Question> findByStatus(QuestionStatus status) { return map(questions.findByStatus(DomainMapper.toColumn(status))); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Question> findByTopicId(Long topicId) { return map(questions.findByTopic_Id(topicId)); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Question> findPublishedByCompetencyId(Long competencyId) {
        return map(questions.findByStatusAndCompetencies_Id(StatusColumn.PUBLISHED, competencyId));
    }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public boolean existsById(Long id) { return questions.existsById(id); }

    /** {@inheritDoc} */
    @Override
    public void deleteById(Long id) {
        questions.deleteById(id);
        questions.flush();
    }

    private static List<Question> map(List<QuestionRecord> records) {
        return records.stream().map(DomainMapper::toDomain).toList();
    }
}
