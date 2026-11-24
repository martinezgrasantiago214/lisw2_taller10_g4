package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.frontend.model.Models.AssignmentView;
import co.unicauca.saberpro.frontend.model.Models.EvaluationView;
import co.unicauca.saberpro.frontend.model.Models.QuestionVersionView;
import co.unicauca.saberpro.frontend.model.Models.ReviewHistoryView;
import co.unicauca.saberpro.frontend.model.Models.RoundView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * HU06: textos del historial de revisiones (logica sin Swing, para probarla).
 * Cada ronda corresponde a una version de la pregunta.
 */
public final class ReviewHistoryFormatter {

    private ReviewHistoryFormatter() {
    }

    /** Titulo de una ronda, p. ej. "Ronda 2 - version 2 - Rechazada". */
    public static String roundTitle(RoundView round) {
        return "Ronda " + round.round() + " - version " + round.questionVersion() + " - " + round.statusLabel();
    }

    /** Detalle de una ronda: envio, revisores y, si hay, el veredicto con sus observaciones. */
    public static String roundDetail(RoundView round) {
        StringBuilder sb = new StringBuilder();
        sb.append("Enviada a revision: ").append(Formats.dateTime(round.submittedAt())).append('\n');
        sb.append("Pregunta: ").append(round.directQuestion() == null ? "" : round.directQuestion()).append('\n');
        if (round.reviewers().isEmpty()) {
            sb.append("Revisores: (aun sin asignar)\n");
        } else {
            sb.append("Revisores: ").append(round.reviewers().stream().map(AssignmentView::reviewerLogin)
                    .collect(Collectors.joining(", "))).append(" (asignados por ")
                    .append(round.reviewers().get(0).assignedBy()).append(")\n");
        }
        EvaluationView e = round.evaluation();
        if (e == null) {
            sb.append("\nSin veredicto todavia.");
        } else {
            sb.append("\nVeredicto: ").append(e.verdict() != null ? e.verdict().getLabel() : "")
                    .append(" por ").append(e.reviewerLogin()).append(" el ")
                    .append(Formats.dateTime(e.evaluatedAt())).append('\n');
            sb.append("Observaciones:\n").append(e.observations() == null || e.observations().isBlank()
                    ? "(el revisor no dejo observaciones)" : e.observations());
        }
        return sb.toString();
    }

    /** Observaciones del ultimo rechazo, para mostrarlas al crear la version mejorada. */
    public static Optional<String> lastRejectionObservations(ReviewHistoryView history) {
        List<RoundView> rounds = history.rounds();
        for (int i = rounds.size() - 1; i >= 0; i--) {
            EvaluationView e = rounds.get(i).evaluation();
            if (e != null && e.verdict() == EvaluationVerdict.RECHAZADA) {
                return Optional.ofNullable(e.observations());
            }
        }
        return Optional.empty();
    }

    /** Resumen de una linea del historial completo. */
    public static String summary(ReviewHistoryView history) {
        long rejected = history.rounds().stream()
                .filter(r -> r.evaluation() != null && r.evaluation().verdict() == EvaluationVerdict.RECHAZADA).count();
        return history.rounds().size() + " ronda(s) de revision, " + rejected + " rechazo(s). Estado actual: "
                + history.currentStatusLabel() + " (version " + history.currentQuestionVersion() + ").";
    }

    /** Contenido de una version anterior archivada. */
    public static String version(QuestionVersionView v) {
        StringBuilder sb = new StringBuilder();
        sb.append("Version ").append(v.version()).append(" - ").append(v.finalStatusLabel() == null ? ""
                : v.finalStatusLabel()).append(" - archivada el ").append(Formats.localDateTime(v.archivedAt()))
                .append("\n\n");
        sb.append("CONTEXTO\n").append(v.context() == null ? "" : v.context()).append("\n\n");
        sb.append("PREGUNTA DIRECTA\n").append(v.directQuestion() == null ? "" : v.directQuestion()).append("\n\n");
        sb.append("OPCIONES\n");
        List<String> options = v.distractors() != null ? v.distractors() : List.of();
        for (int i = 0; i < options.size(); i++) {
            sb.append("  ").append((char) ('A' + i)).append(") ").append(options.get(i));
            if (options.get(i).equals(v.correctAnswer())) {
                sb.append("   <-- respuesta correcta");
            }
            sb.append('\n');
        }
        sb.append("\nJUSTIFICACION\n").append(v.justification() == null ? "" : v.justification()).append('\n');
        return sb.toString();
    }
}
