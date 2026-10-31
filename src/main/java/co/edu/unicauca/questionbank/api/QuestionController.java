package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.application.QuestionService;
import co.edu.unicauca.questionbank.application.ReviewService;
import co.edu.unicauca.questionbank.domain.model.AnswerOption;
import co.edu.unicauca.questionbank.domain.model.DifficultyLevel;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** REST endpoints for questions: creation, queries, classification and lifecycle. */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService questions;
    private final ReviewService reviews;

    /**
     * Creates the controller with its injected dependencies.
     *
     * @param questions author-side use cases
     * @param reviews   reviewer-side use cases
     */
    public QuestionController(QuestionService questions, ReviewService reviews) {
        this.questions = questions;
        this.reviews = reviews;
    }

    /**
     * Returns the created question.
     *
     * @param r multiple-choice question data
     * @return the created question
     */
    @PostMapping("/multiple-choice") @ResponseStatus(HttpStatus.CREATED)
    public Question createMultipleChoice(@RequestBody MultipleChoiceRequest r) {
        List<AnswerOption> options = r.options() == null ? List.of()
            : r.options().stream().map(o -> new AnswerOption(o.label(), o.text(), o.correct())).toList();
        return questions.createMultipleChoiceQuestion(r.statement(), r.difficulty(), r.authorId(), r.topicId(),
            options, r.competencyIds());
    }

    /**
     * Returns the created question.
     *
     * @param r true/false question data
     * @return the created question
     */
    @PostMapping("/true-false") @ResponseStatus(HttpStatus.CREATED)
    public Question createTrueFalse(@RequestBody TrueFalseRequest r) {
        return questions.createTrueFalseQuestion(r.statement(), r.difficulty(), r.authorId(), r.topicId(),
            r.expectedValue(), r.competencyIds());
    }

    /**
     * Returns the created question.
     *
     * @param r open-ended question data
     * @return the created question
     */
    @PostMapping("/open") @ResponseStatus(HttpStatus.CREATED)
    public Question createOpen(@RequestBody OpenRequest r) {
        return questions.createOpenQuestion(r.statement(), r.difficulty(), r.authorId(), r.topicId(),
            r.expectedAnswer(), r.maxWords(), r.competencyIds());
    }

    /**
     * Lists questions, optionally filtered by one criterion.
     *
     * @param authorId optional author filter
     * @param status   optional state filter
     * @param topicId  optional topic filter
     * @return matching questions
     */
    @GetMapping
    public List<Question> list(@RequestParam(required = false) Long authorId,
                               @RequestParam(required = false) QuestionStatus status,
                               @RequestParam(required = false) Long topicId) {
        if (authorId != null) return questions.listByAuthor(authorId);
        if (status != null) return questions.listByStatus(status);
        if (topicId != null) return questions.listByTopic(topicId);
        return questions.listQuestions();
    }

    /**
     * Returns published questions that assess the competency.
     *
     * @param competencyId competency identifier
     * @return published questions that assess the competency
     */
    @GetMapping("/published")
    public List<Question> published(@RequestParam Long competencyId) { return questions.listPublishedByCompetency(competencyId); }

    /**
     * Returns the question.
     *
     * @param id question identifier
     * @return the question
     */
    @GetMapping("/{id}")
    public Question get(@PathVariable Long id) { return questions.getQuestion(id); }

    /**
     * Returns the classified question.
     *
     * @param id question identifier
     * @param r  topic and competencies
     * @return the classified question
     */
    @PutMapping("/{id}/classification")
    public Question classify(@PathVariable Long id, @RequestBody ClassificationRequest r) {
        return questions.classify(id, r.topicId(), r.competencyIds());
    }

    /**
     * Returns the updated question.
     *
     * @param id question identifier
     * @param r  metadata
     * @return the updated question
     */
    @PutMapping("/{id}/metadata")
    public Question updateMetadata(@PathVariable Long id, @RequestBody MetadataRequest r) {
        return questions.updateMetadata(id, r.lastReviewDate(), r.cognitiveLevel(), r.estimatedTimeMinutes(), r.editorialObservations());
    }

    /**
     * Returns the question in PENDING_REVIEW state.
     *
     * @param id question identifier
     * @return the question in PENDING_REVIEW state
     */
    @PostMapping("/{id}/submit")
    public Question submit(@PathVariable Long id) { return questions.submitForReview(id); }

    /**
     * Returns the question in IN_REVIEW state.
     *
     * @param id question identifier
     * @return the question in IN_REVIEW state
     */
    @PostMapping("/{id}/start-review")
    public Question startReview(@PathVariable Long id) { return reviews.startReview(id); }

    /**
     * Returns the question in APPROVED state.
     *
     * @param id question identifier
     * @return the question in APPROVED state
     */
    @PostMapping("/{id}/approve")
    public Question approve(@PathVariable Long id) { return reviews.approve(id); }

    /**
     * Returns the question in REJECTED state.
     *
     * @param id question identifier
     * @return the question in REJECTED state
     */
    @PostMapping("/{id}/reject")
    public Question reject(@PathVariable Long id) { return reviews.reject(id); }

    /**
     * Returns the question in DRAFT state.
     *
     * @param id question identifier
     * @return the question in DRAFT state
     */
    @PostMapping("/{id}/return-to-draft")
    public Question returnToDraft(@PathVariable Long id) { return reviews.returnToDraft(id); }

    /**
     * Returns the question in PUBLISHED state.
     *
     * @param id question identifier
     * @return the question in PUBLISHED state
     */
    @PostMapping("/{id}/publish")
    public Question publish(@PathVariable Long id) { return reviews.publish(id); }

    /**
     * Returns the question in ARCHIVED state.
     *
     * @param id question identifier
     * @return the question in ARCHIVED state
     */
    @PostMapping("/{id}/archive")
    public Question archive(@PathVariable Long id) { return reviews.archive(id); }

    /**
     * Creates the controller with its injected dependencies.
     *
     * @param id question identifier
     */
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { questions.deleteQuestion(id); }

    /**
     * Request body with the option data.
     *
     * @param label   option label
     * @param text    option text
     * @param correct whether it is correct
     */
    public record OptionRequest(String label, String text, boolean correct) { }

    /**
     * Request body with the multiple choice data.
     *
     * @param statement     statement
     * @param difficulty    difficulty
     * @param authorId      author identifier
     * @param topicId       topic identifier
     * @param options       answer options
     * @param competencyIds competency identifiers
     */
    public record MultipleChoiceRequest(String statement, DifficultyLevel difficulty, Long authorId, Long topicId,
                                        List<OptionRequest> options, Set<Long> competencyIds) { }

    /**
     * Request body with the true false data.
     *
     * @param statement     statement
     * @param difficulty    difficulty
     * @param authorId      author identifier
     * @param topicId       topic identifier
     * @param expectedValue expected truth value
     * @param competencyIds competency identifiers
     */
    public record TrueFalseRequest(String statement, DifficultyLevel difficulty, Long authorId, Long topicId,
                                   boolean expectedValue, Set<Long> competencyIds) { }

    /**
     * Request body with the open data.
     *
     * @param statement      statement
     * @param difficulty     difficulty
     * @param authorId       author identifier
     * @param topicId        topic identifier
     * @param expectedAnswer grading guidelines
     * @param maxWords       maximum words
     * @param competencyIds  competency identifiers
     */
    public record OpenRequest(String statement, DifficultyLevel difficulty, Long authorId, Long topicId,
                              String expectedAnswer, int maxWords, Set<Long> competencyIds) { }

    /**
     * Request body with the classification data.
     *
     * @param topicId       topic identifier
     * @param competencyIds competency identifiers
     */
    public record ClassificationRequest(Long topicId, Set<Long> competencyIds) { }

    /**
     * Request body with the metadata data.
     *
     * @param lastReviewDate        last review date
     * @param cognitiveLevel        cognitive level
     * @param estimatedTimeMinutes  estimated time in minutes
     * @param editorialObservations editorial observations
     */
    public record MetadataRequest(LocalDateTime lastReviewDate, String cognitiveLevel,
                                  Integer estimatedTimeMinutes, String editorialObservations) { }
}
