package co.unicauca.saberpro.question.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.events.DomainEvent;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.QuestionRevisedEvent;
import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.web.ConflictException;
import co.unicauca.saberpro.common.web.ForbiddenOperationException;
import co.unicauca.saberpro.question.TestData;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.QuestionVersion;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import co.unicauca.saberpro.question.domain.factory.QuestionValidationException;
import co.unicauca.saberpro.question.testdoubles.InMemoryQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HU06: el autor lee las observaciones del rechazo, saca una version
 * mejorada y la vuelve a enviar a revision. Se prueba el caso de uso con un
 * repositorio en memoria.
 */
class ReviseQuestionTest {

    private InMemoryQuestionRepository repository;
    private final List<DomainEvent> published = new ArrayList<>();
    private QuestionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryQuestionRepository();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(published::add);
        service = new QuestionApplicationService(repository, new QuestionFactoryRegistry(), bus);
    }

    private Question rejected() {
        Question q = service.create(TestData.validRequest("MULTIPLE_CHOICE"));
        service.submitForReview(q.getId(), "autor1");
        return service.applyEvaluation(new QuestionEvaluatedEvent(q.getId(), 1, "autor1", q.getDirectQuestion(),
                "rev1", EvaluationVerdict.RECHAZADA, "Citar la fuente", Instant.now()));
    }

    private static QuestionRequest corrected() {
        QuestionRequest r = TestData.validRequest("MULTIPLE_CHOICE");
        return new QuestionRequest(null, r.getContext(), "¿Que patron arquitectonico admite plugins?",
                r.getDistractors(), r.getCorrectAnswer(), "Segun Richards (2015), el Microkernel...",
                r.getBibliography(), r.getCompetence(), r.getTopic(), r.getSubtopic(), r.getDifficultyLevel(), null);
    }

    @Test
    void elAutorSacaLaVersion2QueQuedaEnBorradorYArchivaLaVersion1() {
        Question q = rejected();
        assertEquals(QuestionStatus.RECHAZADA, q.getStatus());

        Question v2 = service.revise(q.getId(), corrected(), "autor1");

        assertEquals(2, v2.getVersion());
        assertEquals(QuestionStatus.BORRADOR, v2.getStatus());
        assertEquals("autor1", v2.getAuthorLogin());
        assertEquals("MULTIPLE_CHOICE", v2.getType(), "el tipo no cambia");
        assertEquals(v2.getVersion(), service.findById(q.getId()).getVersion());

        List<QuestionVersion> versions = service.versions(q.getId());
        assertEquals(1, versions.size());
        assertEquals(1, versions.get(0).version());
        assertEquals(QuestionStatus.RECHAZADA, versions.get(0).finalStatus());
        assertEquals(q.getDirectQuestion(), versions.get(0).directQuestion());

        QuestionRevisedEvent event = (QuestionRevisedEvent) published.get(published.size() - 1);
        assertEquals(2, event.questionVersion());
    }

    @Test
    void alReenviarLaVersion2ElEventoLlevaSuVersion() {
        Question q = rejected();
        service.revise(q.getId(), corrected(), "autor1");

        service.submitForReview(q.getId(), "autor1");

        QuestionSubmittedForReviewEvent event = (QuestionSubmittedForReviewEvent) published.get(published.size() - 1);
        assertEquals(2, event.questionVersion());
        assertEquals("¿Que patron arquitectonico admite plugins?", event.directQuestion());
    }

    @Test
    void unVeredictoAtrasadoDeLaVersion1NoCambiaLaVersion2() {
        Question q = rejected();
        service.revise(q.getId(), corrected(), "autor1");
        service.submitForReview(q.getId(), "autor1");

        Question result = service.applyEvaluation(new QuestionEvaluatedEvent(q.getId(), 1, "autor1", "¿?", "rev1",
                EvaluationVerdict.RECHAZADA, "repetido", Instant.now()));
        assertEquals(QuestionStatus.PENDIENTE_REVISION, result.getStatus());

        Question approved = service.applyEvaluation(new QuestionEvaluatedEvent(q.getId(), 2, "autor1", "¿?", "rev2",
                EvaluationVerdict.APROBADA, "", Instant.now()));
        assertEquals(QuestionStatus.APROBADA, approved.getStatus());
    }

    @Test
    void soloElAutorYSoloDesdeRechazada() {
        Question q = rejected();
        assertThrows(ForbiddenOperationException.class, () -> service.revise(q.getId(), corrected(), "autor2"));

        Question draft = service.create(TestData.validRequest("MULTIPLE_CHOICE"));
        assertThrows(ConflictException.class, () -> service.revise(draft.getId(), corrected(), "autor1"));
    }

    @Test
    void laVersionMejoradaPasaPorLaValidacionEstructural() {
        Question q = rejected();
        QuestionRequest invalid = TestData.withCorrectAnswer(corrected(), "No esta entre los distractores");

        assertThrows(QuestionValidationException.class, () -> service.revise(q.getId(), invalid, "autor1"));
        assertEquals(QuestionStatus.RECHAZADA, service.findById(q.getId()).getStatus(), "no se guardo nada");
        assertTrue(service.versions(q.getId()).isEmpty());
    }
}
