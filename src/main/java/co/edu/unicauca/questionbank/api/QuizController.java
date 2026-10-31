package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.application.QuizService;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** REST endpoints for quizzes. */
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService quizzes;

    /**
     * Creates the controller with its injected dependencies.
     *
     * @param quizzes quiz use cases
     */
    public QuizController(QuizService quizzes) { this.quizzes = quizzes; }

    /**
     * Returns the created quiz.
     *
     * @param r quiz data
     * @return the created quiz
     */
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Quiz create(@RequestBody QuizRequest r) { return quizzes.createQuiz(r.title(), r.description(), r.questionIds()); }

    /**
     * Returns every quiz.
     *
     * @return every quiz
     */
    @GetMapping
    public List<Quiz> list() { return quizzes.listQuizzes(); }

    /**
     * Returns the quiz.
     *
     * @param id quiz identifier
     * @return the quiz
     */
    @GetMapping("/{id}")
    public Quiz get(@PathVariable Long id) { return quizzes.getQuiz(id); }

    /**
     * Request body with the quiz data.
     *
     * @param title       title
     * @param description description
     * @param questionIds ordered question identifiers
     */
    public record QuizRequest(String title, String description, List<Long> questionIds) { }
}
