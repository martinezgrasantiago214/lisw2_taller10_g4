package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.TopicRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringTopicDataRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/** JPA implementation of the {@link TopicRepository} domain contract. */
@Repository
@Transactional
public class TopicRepositoryJpa implements TopicRepository {
    private final SpringTopicDataRepository data;

    /**
     * Creates the repository implementation with its injected dependencies.
     *
     * @param data Spring Data repository for topics
     */
    public TopicRepositoryJpa(SpringTopicDataRepository data) { this.data = data; }

    /** {@inheritDoc} */
    @Override
    public Topic save(Topic topic) {
        TopicRecord record;
        if (topic.getId() == null) {
            record = new TopicRecord(topic.getName(), topic.getModule());
        } else {
            record = data.findById(topic.getId())
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topic.getId()));
            record.setName(topic.getName());
            record.setModule(topic.getModule());
        }
        return DomainMapper.toDomain(data.saveAndFlush(record));
    }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public Optional<Topic> findById(Long id) { return data.findById(id).map(DomainMapper::toDomain); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Topic> findAll() { return data.findAll().stream().map(DomainMapper::toDomain).toList(); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public boolean existsById(Long id) { return data.existsById(id); }
}
