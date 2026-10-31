package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.repository.QuizRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuizRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringQuestionDataRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.SpringQuizDataRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** JPA implementation of the {@link QuizRepository} domain contract. */
@Repository
@Transactional
public class QuizRepositoryJpa implements QuizRepository {
    private final SpringQuizDataRepository quizzes;
    private final SpringQuestionDataRepository questions;

    /**
     * Creates the repository implementation with its injected dependencies.
     *
     * @param quizzes   Spring Data repository for quizzes
     * @param questions Spring Data repository for questions
     */
    public QuizRepositoryJpa(SpringQuizDataRepository quizzes, SpringQuestionDataRepository questions) {
        this.quizzes = quizzes;
        this.questions = questions;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the quiz or one of its questions does not exist
     */
    @Override
    public Quiz save(Quiz quiz) {
        QuizRecord record = quiz.getId() == null
            ? new QuizRecord()
            : quizzes.findById(quiz.getId()).orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quiz.getId()));
        record.setTitle(quiz.getTitle());
        record.setDescription(quiz.getDescription());

        Map<Long, QuestionRecord> found = questions.findAllById(quiz.getQuestionIds()).stream()
            .collect(Collectors.toMap(QuestionRecord::getId, Function.identity()));
        if (found.size() != quiz.getQuestionIds().size())
            throw new IllegalArgumentException("One or more questions do not exist: " + quiz.getQuestionIds());

        record.clearQuestions();
        for (int i = 0; i < quiz.getQuestionIds().size(); i++) {
            record.addQuestion(found.get(quiz.getQuestionIds().get(i)), i + 1);
        }
        return DomainMapper.toDomain(quizzes.saveAndFlush(record));
    }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public Optional<Quiz> findById(Long id) { return quizzes.findById(id).map(DomainMapper::toDomain); }

    /** {@inheritDoc} */
    @Override @Transactional(readOnly = true)
    public List<Quiz> findAll() { return quizzes.findAll().stream().map(DomainMapper::toDomain).toList(); }
}
