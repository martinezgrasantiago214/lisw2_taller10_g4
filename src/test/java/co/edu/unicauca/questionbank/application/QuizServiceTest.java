package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.fake.*;
import co.edu.unicauca.questionbank.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Quiz use-case tests with in-memory repositories: only published questions are accepted. */
class QuizServiceTest {
    private InMemoryQuestionRepository questions;
    private QuizService quizzes;

    @BeforeEach
    void setUp() {
        questions = new InMemoryQuestionRepository();
        quizzes = new QuizService(new InMemoryQuizRepository(), questions);
    }

    private Question question(QuestionStatus status) {
        return questions.save(new TrueFalseQuestion(null, "statement", DifficultyLevel.LOW, status, 1L, 1L, true));
    }

    @Test
    void createsAQuizWithPublishedQuestionsInOrder() {
        Question a = question(QuestionStatus.PUBLISHED);
        Question b = question(QuestionStatus.PUBLISHED);
        Quiz quiz = quizzes.createQuiz("Mock 1", null, List.of(b.getId(), a.getId()));
        assertNotNull(quiz.getId());
        assertEquals(List.of(b.getId(), a.getId()), quizzes.getQuiz(quiz.getId()).getQuestionIds());
    }

    @Test
    void rejectsQuestionsThatAreNotPublished() {
        for (QuestionStatus status : Set.of(QuestionStatus.DRAFT, QuestionStatus.PENDING_REVIEW, QuestionStatus.IN_REVIEW,
                QuestionStatus.APPROVED, QuestionStatus.ARCHIVED, QuestionStatus.REJECTED)) {
            Question q = question(status);
            assertThrows(IllegalStateException.class, () -> quizzes.createQuiz("Mock", null, List.of(q.getId())));
        }
        assertTrue(quizzes.listQuizzes().isEmpty());
    }

    @Test
    void rejectsMissingEmptyOrRepeatedQuestions() {
        Question a = question(QuestionStatus.PUBLISHED);
        assertThrows(NotFoundException.class, () -> quizzes.createQuiz("Mock", null, List.of(999L)));
        assertThrows(IllegalArgumentException.class, () -> quizzes.createQuiz("Mock", null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> quizzes.createQuiz("Mock", null, List.of(a.getId(), a.getId())));
    }
}
