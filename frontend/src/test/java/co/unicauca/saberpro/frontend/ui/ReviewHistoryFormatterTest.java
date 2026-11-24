package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.frontend.model.Models.AssignmentView;
import co.unicauca.saberpro.frontend.model.Models.EvaluationView;
import co.unicauca.saberpro.frontend.model.Models.ReviewHistoryView;
import co.unicauca.saberpro.frontend.model.Models.RoundView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU06: textos del historial de revisiones que ve el autor. */
class ReviewHistoryFormatterTest {

    private static final AssignmentView REV1 = new AssignmentView("q-1", "rev1", "admin", "2026-11-20T10:00:00Z",
            1, 1, "RECHAZADA", false);

    private static RoundView rejectedRound() {
        return new RoundView(1, 1, "¿Que patron?", "2026-11-20T09:00:00Z", "RECHAZADA", "Rechazada", List.of(REV1),
                new EvaluationView("e-1", "q-1", "rev1", EvaluationVerdict.RECHAZADA, "Citar la fuente",
                        "2026-11-20T11:00:00Z", 1, 1));
    }

    private static RoundView openRound() {
        return new RoundView(2, 2, "¿Que patron arquitectonico?", "2026-11-21T09:00:00Z", "PENDIENTE_ASIGNACION",
                "Pendiente de asignacion", List.of(), null);
    }

    @Test
    void elTituloDiceRondaVersionYEstado() {
        assertEquals("Ronda 1 - version 1 - Rechazada", ReviewHistoryFormatter.roundTitle(rejectedRound()));
    }

    @Test
    void elDetalleMuestraRevisoresVeredictoYObservaciones() {
        String text = ReviewHistoryFormatter.roundDetail(rejectedRound());

        assertTrue(text.contains("Revisores: rev1 (asignados por admin)"));
        assertTrue(text.contains("Veredicto: Rechazada por rev1"));
        assertTrue(text.contains("Citar la fuente"));
        assertTrue(ReviewHistoryFormatter.roundDetail(openRound()).contains("aun sin asignar"));
        assertTrue(ReviewHistoryFormatter.roundDetail(openRound()).contains("Sin veredicto"));
    }

    @Test
    void lasObservacionesDelUltimoRechazoSonLasQueGuianLaVersionMejorada() {
        ReviewHistoryView history = new ReviewHistoryView("q-1", "autor1", 2, 2, "PENDIENTE_ASIGNACION",
                "Pendiente de asignacion", List.of(rejectedRound(), openRound()));

        assertEquals("Citar la fuente", ReviewHistoryFormatter.lastRejectionObservations(history).orElseThrow());
        assertTrue(ReviewHistoryFormatter.summary(history).startsWith("2 ronda(s) de revision, 1 rechazo(s)"));
        assertTrue(ReviewHistoryFormatter.lastRejectionObservations(new ReviewHistoryView("q", "a", 1, 1, "X", "X",
                List.of(openRound()))).isEmpty());
    }
}
