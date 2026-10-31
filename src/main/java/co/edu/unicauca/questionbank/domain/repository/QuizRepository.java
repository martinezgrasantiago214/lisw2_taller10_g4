package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Quiz;
import java.util.List;
import java.util.Optional;

/** Technology-independent persistence contract for quizzes. */
public interface QuizRepository {
    /**
     * Creates or updates a quiz, preserving the order of its questions.
     *
     * @param quiz quiz to store
     * @return the stored quiz
     */
    Quiz save(Quiz quiz);

    /**
     * Returns the quiz, if it exists.
     *
     * @param id quiz identifier
     * @return the quiz, if it exists
     */
    Optional<Quiz> findById(Long id);

    /**
     * Returns every quiz.
     *
     * @return every quiz
     */
    List<Quiz> findAll();
}
