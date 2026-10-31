package co.edu.unicauca.questionbank.application.fake;

import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory implementation of {@link UserRepository} for technology-free tests. */
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> store = new LinkedHashMap<>();
    private long sequence = 0;

    /** {@inheritDoc} */
    @Override public User save(User user) {
        if (user.getId() == null) user.setId(++sequence);
        store.put(user.getId(), user);
        return user;
    }
    /** {@inheritDoc} */
    @Override public Optional<User> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    /** {@inheritDoc} */
    @Override public List<User> findAll() { return new ArrayList<>(store.values()); }
    /** {@inheritDoc} */
    @Override public boolean existsById(Long id) { return store.containsKey(id); }
}
