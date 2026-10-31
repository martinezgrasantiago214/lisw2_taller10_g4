package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.fake.*;
import co.edu.unicauca.questionbank.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Use-case tests that run against in-memory repositories. They prove that the services depend
 * only on the repository contracts and can be tested without JPA or a database.
 */
class QuestionServiceTest {
    private InMemoryQuestionRepository questions;
    private QuestionService service;
    private ReviewService reviews;
    private User author;
    private User reviewer;
    private Topic topic;
    private Competency competency;

    @BeforeEach
    void setUp() {
        questions = new InMemoryQuestionRepository();
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryTopicRepository topics = new InMemoryTopicRepository();
        InMemoryCompetencyRepository competencies = new InMemoryCompetencyRepository();
        service = new QuestionService(questions, users, topics, competencies);
        reviews = new ReviewService(questions);
        author = users.save(new User(null, "Ana", "ana@x.co", Role.AUTHOR));
        reviewer = users.save(new User(null, "Rafa", "rafa@x.co", Role.REVIEWER));
        topic = topics.save(new Topic(null, "Percentages", "Quantitative reasoning"));
        competency = competencies.save(new Competency(null, "RC-INT", "Interpretation"));
    }

    private List<AnswerOption> fiveOptions() {
        return List.of(new AnswerOption("A", "1", true), new AnswerOption("B", "2", false),
            new AnswerOption("C", "3", false), new AnswerOption("D", "4", false), new AnswerOption("E", "5", false));
    }

    @Test
    void createsAMultipleChoiceQuestionAsDraft() {
        Question q = service.createMultipleChoiceQuestion("x", DifficultyLevel.LOW, author.getId(), topic.getId(),
            fiveOptions(), Set.of(competency.getId()));
        assertNotNull(q.getId());
        assertEquals(QuestionStatus.DRAFT, q.getStatus());
        assertEquals(5, ((MultipleChoiceQuestion) q).getOptions().size());
    }

    @Test
    void rejectsAnAuthorThatDoesNotExist() {
        assertThrows(NotFoundException.class, () -> service.createTrueFalseQuestion("x", DifficultyLevel.LOW, 999L,
            topic.getId(), true, Set.of()));
        assertTrue(questions.findAll().isEmpty());
    }

    @Test
    void rejectsAUserWithoutTheAuthorRole() {
        assertThrows(IllegalArgumentException.class, () -> service.createTrueFalseQuestion("x", DifficultyLevel.LOW,
            reviewer.getId(), topic.getId(), true, Set.of()));
    }

    @Test
    void rejectsUnknownTopicOrCompetency() {
        assertThrows(NotFoundException.class, () -> service.createTrueFalseQuestion("x", DifficultyLevel.LOW,
            author.getId(), 999L, true, Set.of()));
        assertThrows(NotFoundException.class, () -> service.createTrueFalseQuestion("x", DifficultyLevel.LOW,
            author.getId(), topic.getId(), true, Set.of(999L)));
    }

    @Test
    void classifiesAQuestion() {
        Question q = service.createTrueFalseQuestion("x", DifficultyLevel.LOW, author.getId(), topic.getId(), true, Set.of());
        Question classified = service.classify(q.getId(), topic.getId(), Set.of(competency.getId()));
        assertEquals(Set.of(competency.getId()), classified.getCompetencyIds());
        assertThrows(NotFoundException.class, () -> service.classify(q.getId(), topic.getId(), Set.of(999L)));
    }

    @Test
    void submitValidatesMinimumConditions() {
        Question incomplete = service.createMultipleChoiceQuestion("x", DifficultyLevel.LOW, author.getId(),
            topic.getId(), List.of(new AnswerOption("A", "1", true)), Set.of(competency.getId()));
        assertThrows(IllegalStateException.class, () -> service.submitForReview(incomplete.getId()));
        assertEquals(QuestionStatus.DRAFT, service.getQuestion(incomplete.getId()).getStatus());

        Question complete = service.createMultipleChoiceQuestion("y", DifficultyLevel.LOW, author.getId(),
            topic.getId(), fiveOptions(), Set.of(competency.getId()));
        assertEquals(QuestionStatus.PENDING_REVIEW, service.submitForReview(complete.getId()).getStatus());
    }

    @Test
    void queriesByAuthorStatusAndPublishedCompetency() {
        Question published = service.createTrueFalseQuestion("a", DifficultyLevel.LOW, author.getId(), topic.getId(),
            true, Set.of(competency.getId()));
        service.submitForReview(published.getId());
        reviews.startReview(published.getId());
        reviews.approve(published.getId());
        reviews.publish(published.getId());
        service.createTrueFalseQuestion("b", DifficultyLevel.LOW, author.getId(), topic.getId(), false, Set.of(competency.getId()));

        assertEquals(2, service.listByAuthor(author.getId()).size());
        assertEquals(1, service.listByStatus(QuestionStatus.DRAFT).size());
        List<Question> byCompetency = service.listPublishedByCompetency(competency.getId());
        assertEquals(1, byCompetency.size());
        assertEquals(published.getId(), byCompetency.get(0).getId());
    }

    @Test
    void unknownQuestionIsReported() {
        assertThrows(NotFoundException.class, () -> service.getQuestion(42L));
        assertThrows(NotFoundException.class, () -> reviews.approve(42L));
        assertThrows(NotFoundException.class, () -> service.deleteQuestion(42L));
    }
}
