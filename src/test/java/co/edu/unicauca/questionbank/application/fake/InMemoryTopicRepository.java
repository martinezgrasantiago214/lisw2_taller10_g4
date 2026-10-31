package co.edu.unicauca.questionbank.application.fake;

import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory implementation of {@link TopicRepository} for technology-free tests. */
public class InMemoryTopicRepository implements TopicRepository {
    private final Map<Long, Topic> store = new LinkedHashMap<>();
    private long sequence = 0;

    /** {@inheritDoc} */
    @Override public Topic save(Topic topic) {
        if (topic.getId() == null) topic.setId(++sequence);
        store.put(topic.getId(), topic);
        return topic;
    }
    /** {@inheritDoc} */
    @Override public Optional<Topic> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    /** {@inheritDoc} */
    @Override public List<Topic> findAll() { return new ArrayList<>(store.values()); }
    /** {@inheritDoc} */
    @Override public boolean existsById(Long id) { return store.containsKey(id); }
}
