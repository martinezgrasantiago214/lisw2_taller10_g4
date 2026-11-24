package co.unicauca.saberpro.frontend.client;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.frontend.model.Models.AssignmentView;
import co.unicauca.saberpro.frontend.model.Models.EvaluationView;
import co.unicauca.saberpro.frontend.model.Models.PendingQuestionView;
import co.unicauca.saberpro.frontend.model.Models.ReviewHistoryView;

import java.util.List;

/** Operaciones del review-service (via gateway). */
public interface ReviewApi {

    List<PendingQuestionView> pendingQuestions();

    List<AssignmentView> assign(String questionId, List<String> reviewers, String assignedBy);

    List<AssignmentView> assignedTo(String reviewerLogin);

    EvaluationView evaluate(String questionId, String reviewerLogin, EvaluationVerdict verdict, String observations);

    List<EvaluationView> evaluations(String questionId);

    /** HU06: historial de revisiones (rondas, revisores, veredictos y observaciones). */
    ReviewHistoryView history(String questionId);
}
