package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.UserRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringUserDataRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/** JPA implementation of the {@link UserRepository} domain contract. */
@Repository
@Transactional
public class UserRepositoryJpa implements UserRepository {
    private final SpringUserDataRepository data;

    /**
     * Creates the repository implementation with its injected dependencies.
     *
     * @param data Spring Data repository for users
     */
    public UserRepositoryJpa(SpringUserDataRepository data) { this.data = data; }

    /** {@inheritDoc} */
    @Override
    public User save(User user) {
        UserRecord record;
        if (user.getId() == null) {
            record = new UserRecord(user.getFullName(), user.getEmail(), DomainMapper.toColumn(user.getRole()));
        } else {
            record = data.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user.getId()));
            record.setFullName(user.getFullName());
            record.setEmail(user.getEmail());
            record.setRole(DomainMapper.toColumn(user.getRole()));
        }
        return DomainMapper.toDomain(data.saveAndFlush(record));
    }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public Optional<User> findById(Long id) { return data.findById(id).map(DomainMapper::toDomain); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<User> findAll() { return data.findAll().stream().map(DomainMapper::toDomain).toList(); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public boolean existsById(Long id) { return data.existsById(id); }
}
