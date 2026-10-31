package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.model.*;
import co.edu.unicauca.questionbank.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Integration tests of the quiz association entity and its constraints against H2. */
@DataJpaTest
@Import({QuizRepositoryJpa.class, QuestionRepositoryJpa.class, UserRepositoryJpa.class,
         TopicRepositoryJpa.class, CompetencyRepositoryJpa.class})
class QuizRepositoryJpaIntegrationTest {

    @Autowired QuizRepository quizzes;
    @Autowired QuestionRepository questions;
    @Autowired UserRepository users;
    @Autowired TopicRepository topics;
    @Autowired TestEntityManager em;
    @Autowired DataSource dataSource;

    JdbcTemplate jdbc;
    Long first;
    Long second;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        User author = users.save(new User(null, "Ana", "ana.quiz@unicauca.edu.co", Role.AUTHOR));
        Topic topic = topics.save(new Topic(null, "Logic", "Quantitative reasoning"));
        first = questions.save(new TrueFalseQuestion("p or not p", DifficultyLevel.LOW, author.getId(), topic.getId(), true)).getId();
        second = questions.save(new TrueFalseQuestion("p and not p", DifficultyLevel.LOW, author.getId(), topic.getId(), false)).getId();
    }

    @Test
    void storesQuestionsWithTheirPosition() {
        Long id = quizzes.save(new Quiz("Mock", "desc", List.of(second, first))).getId();
        em.clear();

        assertEquals(List.of(second, first), quizzes.findById(id).orElseThrow().getQuestionIds());
        assertEquals(2, jdbc.queryForObject("select count(*) from quiz_questions where quiz_id = ?", Integer.class, id).intValue());
        assertEquals(second, jdbc.queryForObject(
            "select question_id from quiz_questions where quiz_id = ? and question_order = 1", Long.class, id));
    }

    @Test
    void rejectsUnknownQuestions() {
        assertThrows(IllegalArgumentException.class, () -> quizzes.save(new Quiz("Mock", null, List.of(first, 999999L))));
    }

    @Test
    void uniqueConstraintRejectsTheSameQuestionTwiceInAQuiz() {
        Long id = quizzes.save(new Quiz("Mock", null, List.of(first))).getId();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "insert into quiz_questions (quiz_id, question_id, question_order) values (?, ?, 2)", id, first));
    }

    @Test
    void foreignKeyPreventsDeletingAQuestionUsedInAQuiz() {
        quizzes.save(new Quiz("Mock", null, List.of(first)));
        em.clear();
        assertThrows(DataIntegrityViolationException.class, () -> questions.deleteById(first));
    }
}
