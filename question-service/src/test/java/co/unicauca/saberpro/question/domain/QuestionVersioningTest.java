package co.unicauca.saberpro.question.domain;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.domain.state.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** HU06 en el dominio: version mejorada de una pregunta rechazada (patron State + Builder). */
class QuestionVersioningTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 10, 0);

    private static Question in(QuestionStatus status) {
        return Question.builder().type("MULTIPLE_CHOICE").fromRequest(TestData.validRequest("MULTIPLE_CHOICE"))
                .status(status).build();
    }

    private static QuestionRequest corrected() {
        QuestionRequest r = TestData.validRequest("MULTIPLE_CHOICE");
        return new QuestionRequest(r.getType(), r.getContext(), "¿Que patron arquitectonico permite plugins?",
                r.getDistractors(), r.getCorrectAnswer(), "Justificacion corregida con la cita.", r.getBibliography(),
                r.getCompetence(), r.getTopic(), r.getSubtopic(), r.getDifficultyLevel(), "otro-login");
    }

    @Test
    void todaPreguntaNaceEnLaVersion1() {
        Question q = in(QuestionStatus.BORRADOR);
        assertEquals(1, q.getVersion());
        assertNull(q.getRevisedAt());
        assertFalse(q.isRevisable());
    }

    @Test
    void unaPreguntaRechazadaSacaUnaVersionMejoradaQueVuelveABorrador() {
        Question rejected = in(QuestionStatus.RECHAZADA);
        assertTrue(rejected.isRevisable());

        Question v2 = rejected.reviseWith(corrected(), NOW);

        assertEquals(rejected.getId(), v2.getId());
        assertEquals(2, v2.getVersion());
        assertEquals(QuestionStatus.BORRADOR, v2.getStatus());
        assertTrue(v2.isEditable());
        assertEquals(NOW, v2.getRevisedAt());
        assertEquals("¿Que patron arquitectonico permite plugins?", v2.getDirectQuestion());
        assertEquals(rejected.getAuthorLogin(), v2.getAuthorLogin(), "el autor no cambia");
        assertEquals(rejected.getCreatedAt(), v2.getCreatedAt());
        assertEquals(rejected.getType(), v2.getType());
    }

    @Test
    void soloDesdeRechazadaSePuedeSacarUnaVersionMejorada() {
        for (QuestionStatus status : new QuestionStatus[]{QuestionStatus.BORRADOR, QuestionStatus.PENDIENTE_REVISION,
                QuestionStatus.APROBADA, QuestionStatus.ELIMINADA}) {
            Question q = in(status);
            assertFalse(q.isRevisable(), status.name());
            assertThrows(InvalidStateTransitionException.class, () -> q.reviseWith(corrected(), NOW), status.name());
        }
    }

    @Test
    void laVersionMejoradaSigueSuCicloDeVidaNormal() {
        Question v2 = in(QuestionStatus.RECHAZADA).reviseWith(corrected(), NOW);

        v2.submitForReview();
        v2.reject();
        Question v3 = v2.reviseWith(corrected(), NOW.plusDays(1));

        assertEquals(3, v3.getVersion());
    }

    @Test
    void laInstantaneaConservaElContenidoYElEstadoFinal() {
        Question rejected = in(QuestionStatus.RECHAZADA);

        QuestionVersion snapshot = rejected.snapshot(NOW);

        assertEquals(rejected.getId(), snapshot.questionId());
        assertEquals(1, snapshot.version());
        assertEquals(rejected.getDirectQuestion(), snapshot.directQuestion());
        assertEquals(rejected.getDistractors(), snapshot.distractors());
        assertEquals(QuestionStatus.RECHAZADA, snapshot.finalStatus());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.distractors().add("x"));
    }
}
