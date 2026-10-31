package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.application.CatalogService;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** REST endpoints for users, topics and competencies. */
@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;

    /**
     * Creates the controller with its injected dependencies.
     *
     * @param catalog catalog use cases
     */
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    /**
     * Returns the registered user.
     *
     * @param r user data
     * @return the registered user
     */
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    public User registerUser(@RequestBody UserRequest r) { return catalog.registerUser(r.fullName(), r.email(), r.role()); }

    /**
     * Returns every user.
     *
     * @return every user
     */
    @GetMapping("/users")
    public List<User> users() { return catalog.users(); }

    /**
     * Returns the created topic.
     *
     * @param r topic data
     * @return the created topic
     */
    @PostMapping("/topics") @ResponseStatus(HttpStatus.CREATED)
    public Topic createTopic(@RequestBody TopicRequest r) { return catalog.createTopic(r.name(), r.module()); }

    /**
     * Returns every topic.
     *
     * @return every topic
     */
    @GetMapping("/topics")
    public List<Topic> topics() { return catalog.topics(); }

    /**
     * Returns the created competency.
     *
     * @param r competency data
     * @return the created competency
     */
    @PostMapping("/competencies") @ResponseStatus(HttpStatus.CREATED)
    public Competency createCompetency(@RequestBody CompetencyRequest r) { return catalog.createCompetency(r.code(), r.name()); }

    /**
     * Returns every competency.
     *
     * @return every competency
     */
    @GetMapping("/competencies")
    public List<Competency> competencies() { return catalog.competencies(); }

    /**
     * Request body with the user data.
     *
     * @param fullName full name
     * @param email    e-mail
     * @param role     role
     */
    public record UserRequest(String fullName, String email, Role role) { }

    /**
     * Request body with the topic data.
     *
     * @param name   topic name
     * @param module Saber PRO module
     */
    public record TopicRequest(String name, String module) { }

    /**
     * Request body with the competency data.
     *
     * @param code competency code
     * @param name competency name
     */
    public record CompetencyRequest(String code, String name) { }
}
