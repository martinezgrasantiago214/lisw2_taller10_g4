package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.domain.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Teacher-side use cases: build quizzes from published questions only. */
@Service
@Transactional
public class QuizService {
    private final QuizRepository quizzes;
    private final QuestionRepository questions;

    /**
     * Creates the service with its injected dependencies.
     *
     * @param quizzes   quiz repository contract
     * @param questions question repository contract
     */
    public QuizService(QuizRepository quizzes, QuestionRepository questions) {
        this.quizzes = quizzes;
        this.questions = questions;
    }

    /**
     * Use case 5: creates a quiz. Every selected question must exist and be published.
     *
     * @param title       title
     * @param description description
     * @param questionIds ordered question identifiers, at least one
     * @return the stored quiz
     * @throws IllegalArgumentException if no question is given or a question is repeated
     * @throws NotFoundException        if a question does not exist
     * @throws IllegalStateException    if a question is not published
     */
    public Quiz createQuiz(String title, String description, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty())
            throw new IllegalArgumentException("A quiz must contain at least one question");
        Quiz quiz = new Quiz(title, description, questionIds);
        for (Long questionId : quiz.getQuestionIds()) {
            Question question = questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found: " + questionId));
            if (!question.isUsableInQuizzes())
                throw new IllegalStateException("Only approved and published questions can be added to a quiz: " + questionId);
        }
        return quizzes.save(quiz);
    }

    /**
     * Returns the quiz.
     *
     * @param id quiz identifier
     * @return the quiz
     * @throws NotFoundException if it does not exist
     */
    @Transactional(readOnly = true)
    public Quiz getQuiz(Long id) { return quizzes.findById(id).orElseThrow(() -> new NotFoundException("Quiz not found: " + id)); }

    /**
     * Returns every quiz.
     *
     * @return every quiz
     */
    @Transactional(readOnly = true)
    public List<Quiz> listQuizzes() { return quizzes.findAll(); }
}
