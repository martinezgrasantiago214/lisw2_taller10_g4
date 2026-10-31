package co.edu.unicauca.questionbank.application.fake;

import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory implementation of {@link CompetencyRepository} for technology-free tests. */
public class InMemoryCompetencyRepository implements CompetencyRepository {
    private final Map<Long, Competency> store = new LinkedHashMap<>();
    private long sequence = 0;

    /** {@inheritDoc} */
    @Override public Competency save(Competency competency) {
        if (competency.getId() == null) competency.setId(++sequence);
        store.put(competency.getId(), competency);
        return competency;
    }
    /** {@inheritDoc} */
    @Override public Optional<Competency> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    /** {@inheritDoc} */
    @Override public List<Competency> findAll() { return new ArrayList<>(store.values()); }
    /** {@inheritDoc} */
    @Override public boolean existsById(Long id) { return store.containsKey(id); }
}
