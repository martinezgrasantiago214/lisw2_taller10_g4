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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests of the JPA persistence layer against H2. They inject the domain
 * repository INTERFACES; Spring supplies the JPA implementations. After each save the
 * persistence context is cleared and the tables are checked with plain SQL, so the tests
 * verify what is really stored and not what Hibernate keeps in memory.
 */
@DataJpaTest
@Import({QuestionRepositoryJpa.class, UserRepositoryJpa.class, TopicRepositoryJpa.class, CompetencyRepositoryJpa.class})
class QuestionRepositoryJpaIntegrationTest {

    @Autowired QuestionRepository questions;
    @Autowired UserRepository users;
    @Autowired TopicRepository topics;
    @Autowired CompetencyRepository competencies;
    @Autowired TestEntityManager em;
    @Autowired DataSource dataSource;

    JdbcTemplate jdbc;
    User author;
    Topic quantitative;
    Topic writing;
    Competency interpret;
    Competency formulate;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        author = users.save(new User(null, "Ana Author", "ana@unicauca.edu.co", Role.AUTHOR));
        quantitative = topics.save(new Topic(null, "Percentages", "Quantitative reasoning"));
        writing = topics.save(new Topic(null, "Argumentation", "Written communication"));
        interpret = competencies.save(new Competency(null, "RC-INT", "Interpretation"));
        formulate = competencies.save(new Competency(null, "RC-FOR", "Formulation"));
    }

    private MultipleChoiceQuestion newMultipleChoice() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion("15% of 80,000?", DifficultyLevel.LOW, author.getId(), quantitative.getId());
        q.addOption(new AnswerOption("B", "10,000", false));
        q.addOption(new AnswerOption("A", "12,000", true));
        q.addOption(new AnswerOption("C", "15,000", false));
        q.addOption(new AnswerOption("D", "8,000", false));
        q.addOption(new AnswerOption("E", "9,000", false));
        return q;
    }

    private TrueFalseQuestion newTrueFalse() {
        return new TrueFalseQuestion("The Earth orbits the Sun", DifficultyLevel.LOW, author.getId(), quantitative.getId(), true);
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    private Question publish(Question q) {
        q.submitForReview();
        q.startReview();
        q.approve();
        q.publish();
        return questions.save(q);
    }

    // ------------------------------------------------------------------ save and retrieve

    @Test
    void savesAndRetrievesEachQuestionType() {
        Long mcId = questions.save(newMultipleChoice()).getId();
        Long tfId = questions.save(newTrueFalse()).getId();
        Long openId = questions.save(new OpenQuestion("Write an essay", DifficultyLevel.MEDIUM, author.getId(),
            writing.getId(), "Position and two arguments", 300)).getId();
        em.clear();

        assertInstanceOf(MultipleChoiceQuestion.class, questions.findById(mcId).orElseThrow());
        TrueFalseQuestion tf = (TrueFalseQuestion) questions.findById(tfId).orElseThrow();
        assertTrue(tf.getExpectedValue());
        OpenQuestion open = (OpenQuestion) questions.findById(openId).orElseThrow();
        assertEquals(300, open.getMaxWords());
        assertEquals(3, questions.findAll().size());
    }

    // ------------------------------------------------------------------ inheritance (JOINED)

    @Test
    void joinedStoresOneRowInTheBaseTableAndOneInTheSubtypeTable() {
        Long mcId = questions.save(newMultipleChoice()).getId();
        Long tfId = questions.save(newTrueFalse()).getId();
        em.clear();

        assertEquals(2, count("select count(*) from questions"));
        assertEquals(1, count("select count(*) from multiple_choice_questions where question_id = ?", mcId));
        assertEquals(1, count("select count(*) from true_false_questions where question_id = ?", tfId));
        assertEquals(0, count("select count(*) from true_false_questions where question_id = ?", mcId));
    }

    // ------------------------------------------------------------------ one-to-many

    @Test
    void oneToManyKeepsTheOptionsOrderedWithTheirQuestion() {
        Long id = questions.save(newMultipleChoice()).getId();
        em.clear();

        MultipleChoiceQuestion loaded = (MultipleChoiceQuestion) questions.findById(id).orElseThrow();
        assertEquals(List.of("A", "B", "C", "D", "E"), loaded.getOptions().stream().map(AnswerOption::getLabel).toList());
        assertEquals("12,000", loaded.findCorrectOption().orElseThrow().getText());
        assertEquals(5, count("select count(*) from answer_options where question_id = ?", id));
        assertEquals(1, count("select count(*) from answer_options where question_id = ? and is_correct = true", id));
    }

    @Test
    void orphanRemovalDeletesReplacedOptions() {
        MultipleChoiceQuestion saved = (MultipleChoiceQuestion) questions.save(newMultipleChoice());
        saved.replaceOptions(List.of(new AnswerOption("A", "1", true), new AnswerOption("B", "2", false),
            new AnswerOption("C", "3", false), new AnswerOption("D", "4", false), new AnswerOption("E", "5", false)));
        questions.save(saved);
        em.clear();

        assertEquals(5, count("select count(*) from answer_options"));
        assertEquals(1, count("select count(*) from answer_options where option_text = '1'"));
    }

    @Test
    void stateChangesDoNotRewriteUnchangedOptions() {
        MultipleChoiceQuestion q = newMultipleChoice();
        q.addCompetency(interpret.getId());
        Question saved = questions.save(q);
        Long firstOptionId = ((MultipleChoiceQuestion) saved).getOptions().get(0).getId();
        saved.submitForReview();
        Question submitted = questions.save(saved);
        assertEquals(firstOptionId, ((MultipleChoiceQuestion) submitted).getOptions().get(0).getId());
    }

    // ------------------------------------------------------------------ many-to-one

    @Test
    void manyToOneStoresAuthorAndTopicForeignKeys() {
        Long id = questions.save(newMultipleChoice()).getId();
        em.clear();
        assertEquals(author.getId(), jdbc.queryForObject("select author_id from questions where id = ?", Long.class, id));
        assertEquals(quantitative.getId(), jdbc.queryForObject("select topic_id from questions where id = ?", Long.class, id));
    }

    // ------------------------------------------------------------------ many-to-many

    @Test
    void manyToManyStoresCompetenciesInTheJoinTable() {
        MultipleChoiceQuestion q = newMultipleChoice();
        q.addCompetency(interpret.getId());
        q.addCompetency(formulate.getId());
        Long id = questions.save(q).getId();
        em.clear();

        assertEquals(2, count("select count(*) from question_competencies where question_id = ?", id));
        assertEquals(Set.of(interpret.getId(), formulate.getId()), questions.findById(id).orElseThrow().getCompetencyIds());
    }

    // ------------------------------------------------------------------ one-to-one

    @Test
    void oneToOneStoresOptionalMetadata() {
        MultipleChoiceQuestion withMetadata = newMultipleChoice();
        LocalDateTime reviewDate = LocalDateTime.of(2026, 10, 30, 10, 30);
        withMetadata.setMetadata(new QuestionMetadata(reviewDate, "ANALYSIS", 4, "Clarify the wording"));
        Long id = questions.save(withMetadata).getId();
        Long withoutId = questions.save(newTrueFalse()).getId();
        em.clear();

        QuestionMetadata loaded = questions.findById(id).orElseThrow().getMetadata();
        assertNotNull(loaded);
        assertEquals(reviewDate, loaded.getLastReviewDate());
        assertEquals("ANALYSIS", loaded.getCognitiveLevel());
        assertNull(questions.findById(withoutId).orElseThrow().getMetadata());
        assertEquals(1, count("select count(*) from question_metadata"));
    }

    // ------------------------------------------------------------------ repository queries

    @Test
    void queriesByAuthorStatusTopicAndPublishedCompetency() {
        MultipleChoiceQuestion published = newMultipleChoice();
        published.addCompetency(interpret.getId());
        Long publishedId = publish(questions.save(published)).getId();

        TrueFalseQuestion draft = newTrueFalse();
        draft.addCompetency(interpret.getId());
        questions.save(draft);
        em.clear();

        assertEquals(2, questions.findByAuthorId(author.getId()).size());
        assertEquals(1, questions.findByStatus(QuestionStatus.PUBLISHED).size());
        assertEquals(1, questions.findByStatus(QuestionStatus.DRAFT).size());
        assertEquals(2, questions.findByTopicId(quantitative.getId()).size());
        List<Question> byCompetency = questions.findPublishedByCompetencyId(interpret.getId());
        assertEquals(1, byCompetency.size());
        assertEquals(publishedId, byCompetency.get(0).getId());
        assertEquals("PUBLISHED", jdbc.queryForObject("select status from questions where id = ?", String.class, publishedId));
    }

    // ------------------------------------------------------------------ deletion

    @Test
    void deleteRemovesSubtypeRowOptionsMetadataAndJoinRows() {
        MultipleChoiceQuestion q = newMultipleChoice();
        q.addCompetency(interpret.getId());
        q.setMetadata(new QuestionMetadata(null, "ANALYSIS", 4, null));
        Long id = questions.save(q).getId();
        em.clear();

        questions.deleteById(id);

        assertEquals(0, count("select count(*) from questions"));
        assertEquals(0, count("select count(*) from multiple_choice_questions"));
        assertEquals(0, count("select count(*) from answer_options"));
        assertEquals(0, count("select count(*) from question_metadata"));
        assertEquals(0, count("select count(*) from question_competencies"));
        assertEquals(2, count("select count(*) from competencies"));
    }

    // ------------------------------------------------------------------ invalid relations

    @Test
    void repositoryRejectsReferencesToMissingRows() {
        MultipleChoiceQuestion unknownAuthor = new MultipleChoiceQuestion("x", DifficultyLevel.LOW, 9999L, quantitative.getId());
        assertThrows(IllegalArgumentException.class, () -> questions.save(unknownAuthor));

        MultipleChoiceQuestion unknownTopic = new MultipleChoiceQuestion("x", DifficultyLevel.LOW, author.getId(), 9999L);
        assertThrows(IllegalArgumentException.class, () -> questions.save(unknownTopic));

        MultipleChoiceQuestion unknownCompetency = newMultipleChoice();
        unknownCompetency.addCompetency(9999L);
        assertThrows(IllegalArgumentException.class, () -> questions.save(unknownCompetency));
        assertEquals(0, count("select count(*) from questions"));
    }

    @Test
    void foreignKeyRejectsAnOptionWithoutAnExistingQuestion() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "insert into answer_options (label, option_text, is_correct, question_id) values ('A', 'x', true, 999999)"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "insert into answer_options (label, option_text, is_correct, question_id) values ('A', 'x', true, null)"));
    }

    @Test
    void foreignKeyRejectsAQuestionWithAMissingAuthor() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "insert into questions (statement, difficulty, status, author_id, topic_id) values ('x', 'LOW', 'DRAFT', 999999, ?)",
            quantitative.getId()));
    }

    @Test
    void uniqueConstraintAllowsOnlyOneMetadataRowPerQuestion() {
        MultipleChoiceQuestion q = newMultipleChoice();
        q.setMetadata(new QuestionMetadata(null, "ANALYSIS", 4, null));
        Long id = questions.save(q).getId();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "insert into question_metadata (question_id, cognitive_level) values (?, 'RECALL')", id));
    }
}
