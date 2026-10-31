package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.CompetencyRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringCompetencyDataRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/** JPA implementation of the {@link CompetencyRepository} domain contract. */
@Repository
@Transactional
public class CompetencyRepositoryJpa implements CompetencyRepository {
    private final SpringCompetencyDataRepository data;

    /**
     * Creates the repository implementation with its injected dependencies.
     *
     * @param data Spring Data repository for competencies
     */
    public CompetencyRepositoryJpa(SpringCompetencyDataRepository data) { this.data = data; }

    /** {@inheritDoc} */
    @Override
    public Competency save(Competency competency) {
        CompetencyRecord record;
        if (competency.getId() == null) {
            record = new CompetencyRecord(competency.getCode(), competency.getName());
        } else {
            record = data.findById(competency.getId())
                .orElseThrow(() -> new IllegalArgumentException("Competency not found: " + competency.getId()));
            record.setCode(competency.getCode());
            record.setName(competency.getName());
        }
        return DomainMapper.toDomain(data.saveAndFlush(record));
    }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public Optional<Competency> findById(Long id) { return data.findById(id).map(DomainMapper::toDomain); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Competency> findAll() { return data.findAll().stream().map(DomainMapper::toDomain).toList(); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public boolean existsById(Long id) { return data.existsById(id); }
}
