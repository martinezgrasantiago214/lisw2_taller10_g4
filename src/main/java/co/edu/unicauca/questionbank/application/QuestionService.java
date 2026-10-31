package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.domain.model.*;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Author-side use cases: create, query, classify and submit questions. It depends only on
 * domain repository contracts (dependency inversion), never on JPA or Spring Data.
 */
@Service
@Transactional
public class QuestionService {
    private final QuestionRepository questions;
    private final UserRepository users;
    private final TopicRepository topics;
    private final CompetencyRepository competencies;

    /**
     * Creates the service with its injected dependencies.
     *
     * @param questions    question repository contract
     * @param users        user repository contract
     * @param topics       topic repository contract
     * @param competencies competency repository contract
     */
    public QuestionService(QuestionRepository questions, UserRepository users,
                           TopicRepository topics, CompetencyRepository competencies) {
        this.questions = questions;
        this.users = users;
        this.topics = topics;
        this.competencies = competencies;
    }

    /**
     * Use case 1: creates a multiple-choice question with its options.
     *
     * @param statement     statement
     * @param difficulty    difficulty level
     * @param authorId      identifier of a user with the AUTHOR role
     * @param topicId       topic identifier
     * @param options       answer options
     * @param competencyIds competency identifiers, may be empty
     * @return the stored question, in DRAFT state
     */
    public Question createMultipleChoiceQuestion(String statement, DifficultyLevel difficulty, Long authorId,
                                                 Long topicId, List<AnswerOption> options, Set<Long> competencyIds) {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion(statement, difficulty, authorId, topicId);
        if (options != null) options.forEach(q::addOption);
        return storeNew(q, competencyIds);
    }

    /**
     * Use case 1: creates a true/false question.
     *
     * @param statement     statement
     * @param difficulty    difficulty level
     * @param authorId      identifier of a user with the AUTHOR role
     * @param topicId       topic identifier
     * @param expectedValue expected truth value
     * @param competencyIds competency identifiers, may be empty
     * @return the stored question, in DRAFT state
     */
    public Question createTrueFalseQuestion(String statement, DifficultyLevel difficulty, Long authorId,
                                            Long topicId, boolean expectedValue, Set<Long> competencyIds) {
        return storeNew(new TrueFalseQuestion(statement, difficulty, authorId, topicId, expectedValue), competencyIds);
    }

    /**
     * Use case 1: creates an open-ended question.
     *
     * @param statement      statement
     * @param difficulty     difficulty level
     * @param authorId       identifier of a user with the AUTHOR role
     * @param topicId        topic identifier
     * @param expectedAnswer grading guidelines
     * @param maxWords       maximum number of words
     * @param competencyIds  competency identifiers, may be empty
     * @return the stored question, in DRAFT state
     */
    public Question createOpenQuestion(String statement, DifficultyLevel difficulty, Long authorId, Long topicId,
                                       String expectedAnswer, int maxWords, Set<Long> competencyIds) {
        return storeNew(new OpenQuestion(statement, difficulty, authorId, topicId, expectedAnswer, maxWords), competencyIds);
    }

    private Question storeNew(Question q, Set<Long> competencyIds) {
        requireAuthor(q.getAuthorId());
        requireTopic(q.getTopicId());
        if (competencyIds != null) {
            requireCompetencies(competencyIds);
            competencyIds.forEach(q::addCompetency);
        }
        return questions.save(q);
    }

    /**
     * Use case 2: finds a question by identifier.
     *
     * @param id question identifier
     * @return the question
     * @throws NotFoundException if it does not exist
     */
    @Transactional(readOnly = true)
    public Question getQuestion(Long id) {
        return questions.findById(id).orElseThrow(() -> new NotFoundException("Question not found: " + id));
    }

    /**
     * Returns every question (use case 2).
     *
     * @return every question (use case 2)
     */
    @Transactional(readOnly = true)
    public List<Question> listQuestions() { return questions.findAll(); }

    /**
     * Returns questions written by that author (use case 2).
     *
     * @param authorId author identifier
     * @return questions written by that author (use case 2)
     */
    @Transactional(readOnly = true)
    public List<Question> listByAuthor(Long authorId) { return questions.findByAuthorId(authorId); }

    /**
     * Returns questions in that state (use case 2).
     *
     * @param status lifecycle state
     * @return questions in that state (use case 2)
     */
    @Transactional(readOnly = true)
    public List<Question> listByStatus(QuestionStatus status) { return questions.findByStatus(status); }

    /**
     * Returns questions of that topic (use case 2).
     *
     * @param topicId topic identifier
     * @return questions of that topic (use case 2)
     */
    @Transactional(readOnly = true)
    public List<Question> listByTopic(Long topicId) { return questions.findByTopicId(topicId); }

    /**
     * Returns published questions that assess that competency (use case 2).
     *
     * @param competencyId competency identifier
     * @return published questions that assess that competency (use case 2)
     */
    @Transactional(readOnly = true)
    public List<Question> listPublishedByCompetency(Long competencyId) { return questions.findPublishedByCompetencyId(competencyId); }

    /**
     * Use case 3: classifies a question under a topic and one or more competencies.
     *
     * @param questionId    question identifier
     * @param topicId       topic identifier
     * @param competencyIds competency identifiers, at least one
     * @return the updated question
     * @throws NotFoundException if the question, topic or a competency does not exist
     */
    public Question classify(Long questionId, Long topicId, Set<Long> competencyIds) {
        Question q = getQuestion(questionId);
        requireTopic(topicId);
        requireCompetencies(competencyIds);
        q.classify(topicId, competencyIds);
        return questions.save(q);
    }

    /**
     * Use case 4: submits a draft for review after checking its minimum conditions.
     * Loading, validating and saving run in one transaction.
     *
     * @param questionId question identifier
     * @return the question in PENDING_REVIEW state
     * @throws IllegalStateException if the question does not meet the conditions
     */
    public Question submitForReview(Long questionId) {
        Question q = getQuestion(questionId);
        q.submitForReview();
        return questions.save(q);
    }

    /**
     * Adds or replaces the optional metadata of a question.
     *
     * @param questionId            question identifier
     * @param lastReviewDate        last review date
     * @param cognitiveLevel        cognitive level
     * @param estimatedTimeMinutes  estimated time in minutes
     * @param editorialObservations editorial observations
     * @return the updated question
     */
    public Question updateMetadata(Long questionId, LocalDateTime lastReviewDate, String cognitiveLevel,
                                   Integer estimatedTimeMinutes, String editorialObservations) {
        Question q = getQuestion(questionId);
        q.setMetadata(new QuestionMetadata(lastReviewDate, cognitiveLevel, estimatedTimeMinutes, editorialObservations));
        return questions.save(q);
    }

    /**
     * Deletes a question together with its options, metadata and competency links.
     *
     * @param id question identifier
     * @throws NotFoundException if it does not exist
     */
    public void deleteQuestion(Long id) {
        if (!questions.existsById(id)) throw new NotFoundException("Question not found: " + id);
        questions.deleteById(id);
    }

    private void requireAuthor(Long authorId) {
        User author = users.findById(authorId).orElseThrow(() -> new NotFoundException("Author not found: " + authorId));
        if (!author.canAuthorQuestions())
            throw new IllegalArgumentException("User " + authorId + " does not have the AUTHOR role");
    }

    private void requireTopic(Long topicId) {
        if (topicId == null || !topics.existsById(topicId)) throw new NotFoundException("Topic not found: " + topicId);
    }

    private void requireCompetencies(Collection<Long> competencyIds) {
        if (competencyIds == null) return;
        for (Long id : competencyIds) {
            if (id == null || !competencies.existsById(id)) throw new NotFoundException("Competency not found: " + id);
        }
    }
}
