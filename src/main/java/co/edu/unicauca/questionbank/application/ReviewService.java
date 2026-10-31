package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.function.Consumer;

/**
 * Reviewer-side use cases: lifecycle transitions after submission. Each transition loads the
 * question, lets the domain validate it and saves it inside one transaction; no state column is
 * ever updated directly.
 */
@Service
@Transactional
public class ReviewService {
    private final QuestionRepository questions;

    /**
     * Creates the service with its injected dependencies.
     *
     * @param questions question repository contract
     */
    public ReviewService(QuestionRepository questions) { this.questions = questions; }

    /**
     * Returns the question in IN_REVIEW state.
     *
     * @param id question identifier
     * @return the question in IN_REVIEW state
     */
    public Question startReview(Long id) { return transition(id, Question::startReview); }

    /**
     * Returns the question in APPROVED state.
     *
     * @param id question identifier
     * @return the question in APPROVED state
     */
    public Question approve(Long id) { return transition(id, Question::approve); }

    /**
     * Returns the question in REJECTED state.
     *
     * @param id question identifier
     * @return the question in REJECTED state
     */
    public Question reject(Long id) { return transition(id, Question::reject); }

    /**
     * Returns the question back in DRAFT state.
     *
     * @param id question identifier
     * @return the question back in DRAFT state
     */
    public Question returnToDraft(Long id) { return transition(id, Question::returnToDraft); }

    /**
     * Returns the question in PUBLISHED state.
     *
     * @param id question identifier
     * @return the question in PUBLISHED state
     */
    public Question publish(Long id) { return transition(id, Question::publish); }

    /**
     * Returns the question in ARCHIVED state.
     *
     * @param id question identifier
     * @return the question in ARCHIVED state
     */
    public Question archive(Long id) { return transition(id, Question::archive); }

    private Question transition(Long id, Consumer<Question> action) {
        Question q = questions.findById(id).orElseThrow(() -> new NotFoundException("Question not found: " + id));
        action.accept(q);
        return questions.save(q);
    }
}
