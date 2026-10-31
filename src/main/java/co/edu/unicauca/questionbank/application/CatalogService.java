package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Use cases for the reference data: users, topics and competencies. */
@Service
@Transactional
public class CatalogService {
    private final UserRepository users;
    private final TopicRepository topics;
    private final CompetencyRepository competencies;

    /**
     * Creates the service with its injected dependencies.
     *
     * @param users        user repository contract
     * @param topics       topic repository contract
     * @param competencies competency repository contract
     */
    public CatalogService(UserRepository users, TopicRepository topics, CompetencyRepository competencies) {
        this.users = users;
        this.topics = topics;
        this.competencies = competencies;
    }

    /**
     * Returns the registered user.
     *
     * @param fullName full name
     * @param email    unique e-mail
     * @param role     role
     * @return the registered user
     */
    public User registerUser(String fullName, String email, Role role) { return users.save(new User(null, fullName, email, role)); }

    /**
     * Returns the created topic.
     *
     * @param name   unique topic name
     * @param module Saber PRO module
     * @return the created topic
     */
    public Topic createTopic(String name, String module) { return topics.save(new Topic(null, name, module)); }

    /**
     * Returns the created competency.
     *
     * @param code unique competency code
     * @param name competency name
     * @return the created competency
     */
    public Competency createCompetency(String code, String name) { return competencies.save(new Competency(null, code, name)); }

    /**
     * Returns every user.
     *
     * @return every user
     */
    @Transactional(readOnly = true)
    public List<User> users() { return users.findAll(); }

    /**
     * Returns every topic.
     *
     * @return every topic
     */
    @Transactional(readOnly = true)
    public List<Topic> topics() { return topics.findAll(); }

    /**
     * Returns every competency.
     *
     * @return every competency
     */
    @Transactional(readOnly = true)
    public List<Competency> competencies() { return competencies.findAll(); }
}
