package co.edu.unicauca.questionbank.domain;

import co.edu.unicauca.questionbank.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests of the domain model: no Spring, no JPA, no database. */
class QuestionTest {

    private MultipleChoiceQuestion completeQuestion() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion("2 + 2 = ?", DifficultyLevel.LOW, 1L, 1L);
        q.addOption(new AnswerOption("A", "4", true));
        q.addOption(new AnswerOption("B", "3", false));
        q.addOption(new AnswerOption("C", "5", false));
        q.addOption(new AnswerOption("D", "6", false));
        q.addOption(new AnswerOption("E", "7", false));
        return q;
    }

    @Test
    void newQuestionStartsAsDraft() {
        assertEquals(QuestionStatus.DRAFT, completeQuestion().getStatus());
    }

    @Test
    void requiredFieldsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new MultipleChoiceQuestion(" ", DifficultyLevel.LOW, 1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> new MultipleChoiceQuestion("x", null, 1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> new MultipleChoiceQuestion("x", DifficultyLevel.LOW, null, 1L));
        assertThrows(IllegalArgumentException.class, () -> new MultipleChoiceQuestion("x", DifficultyLevel.LOW, 1L, null));
    }

    @Test
    void multipleChoiceNeedsFiveOptionsAndExactlyOneCorrect() {
        MultipleChoiceQuestion four = new MultipleChoiceQuestion("x", DifficultyLevel.LOW, 1L, 1L);
        four.addOption(new AnswerOption("A", "a", true));
        four.addOption(new AnswerOption("B", "b", false));
        four.addOption(new AnswerOption("C", "c", false));
        four.addOption(new AnswerOption("D", "d", false));
        assertFalse(four.isReadyForReview());

        MultipleChoiceQuestion twoCorrect = new MultipleChoiceQuestion("x", DifficultyLevel.LOW, 1L, 1L);
        twoCorrect.replaceOptions(List.of(new AnswerOption("A", "a", true), new AnswerOption("B", "b", true),
            new AnswerOption("C", "c", false), new AnswerOption("D", "d", false), new AnswerOption("E", "e", false)));
        assertFalse(twoCorrect.isReadyForReview());

        assertTrue(completeQuestion().isReadyForReview());
    }

    @Test
    void rejectsDuplicatedLabelsAndASixthOption() {
        MultipleChoiceQuestion q = completeQuestion();
        assertThrows(IllegalArgumentException.class, () -> q.addOption(new AnswerOption("a", "duplicated", false)));
        assertThrows(IllegalStateException.class, () -> q.addOption(new AnswerOption("F", "8", false)));
    }

    @Test
    void submissionRequiresCompleteContentAndACompetency() {
        MultipleChoiceQuestion q = completeQuestion();
        assertThrows(IllegalStateException.class, q::submitForReview);
        assertEquals(QuestionStatus.DRAFT, q.getStatus());

        q.addCompetency(10L);
        q.submitForReview();
        assertEquals(QuestionStatus.PENDING_REVIEW, q.getStatus());
    }

    @Test
    void fullLifecycleFollowsTheAllowedTransitions() {
        MultipleChoiceQuestion q = completeQuestion();
        q.addCompetency(10L);
        assertThrows(IllegalStateException.class, q::approve);
        q.submitForReview();
        assertThrows(IllegalStateException.class, q::approve);
        q.startReview();
        q.approve();
        assertFalse(q.isUsableInQuizzes());
        q.publish();
        assertTrue(q.isUsableInQuizzes());
        q.archive();
        assertEquals(QuestionStatus.ARCHIVED, q.getStatus());
        assertFalse(q.isUsableInQuizzes());
    }

    @Test
    void rejectedQuestionReturnsToDraftAndCanBeCorrected() {
        MultipleChoiceQuestion q = completeQuestion();
        q.addCompetency(2L);
        q.submitForReview();
        q.startReview();
        q.reject();
        assertDoesNotThrow(() -> q.changeStatement("Corrected statement"));
        q.returnToDraft();
        assertEquals(QuestionStatus.DRAFT, q.getStatus());
    }

    @Test
    void contentCannotChangeOnceInReview() {
        MultipleChoiceQuestion q = completeQuestion();
        q.addCompetency(1L);
        q.submitForReview();
        assertThrows(IllegalStateException.class, () -> q.changeStatement("other"));
        assertThrows(IllegalStateException.class, () -> q.classify(2L, Set.of(3L)));
    }

    @Test
    void classifyReplacesTopicAndCompetencies() {
        MultipleChoiceQuestion q = completeQuestion();
        q.addCompetency(1L);
        q.classify(7L, Set.of(2L, 3L));
        assertEquals(7L, q.getTopicId().longValue());
        assertEquals(Set.of(2L, 3L), q.getCompetencyIds());
        assertThrows(IllegalArgumentException.class, () -> q.classify(7L, Set.of()));
    }

    @Test
    void trueFalseAndOpenQuestionsApplyTheirOwnRules() {
        TrueFalseQuestion tf = new TrueFalseQuestion("Java is compiled", DifficultyLevel.LOW, 1L, 1L, true);
        assertTrue(tf.isReadyForReview());
        assertEquals(TrueFalseQuestion.TYPE, tf.getType());

        OpenQuestion open = new OpenQuestion("Write...", DifficultyLevel.MEDIUM, 1L, 1L, "", 0);
        assertFalse(open.isReadyForReview());
        open.changeGuidelines("Position and arguments", 300);
        assertTrue(open.isReadyForReview());
    }

    @Test
    void metadataValidatesItsValues() {
        assertThrows(IllegalArgumentException.class, () -> new QuestionMetadata(null, " ", null, null));
        assertThrows(IllegalArgumentException.class, () -> new QuestionMetadata(null, null, 0, null));
        assertDoesNotThrow(() -> new QuestionMetadata(null, null, null, null));
    }

    @Test
    void quizRejectsRepeatedQuestionsAndKeepsOrder() {
        assertThrows(IllegalArgumentException.class, () -> new Quiz("Mock", null, List.of(1L, 1L)));
        Quiz quiz = new Quiz("Mock", null, List.of(3L, 1L, 2L));
        assertEquals(List.of(3L, 1L, 2L), quiz.getQuestionIds());
    }

    @Test
    void onlyAuthorsCanWriteQuestions() {
        assertTrue(new User(1L, "Ana", "ana@x.co", Role.AUTHOR).canAuthorQuestions());
        assertFalse(new User(2L, "Rafa", "rafa@x.co", Role.REVIEWER).canAuthorQuestions());
    }
}
