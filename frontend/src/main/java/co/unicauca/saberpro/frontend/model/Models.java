package co.unicauca.saberpro.frontend.model;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.contract.Role;

import java.util.List;

/** Modelos del cliente: espejo del JSON que devuelve el API Gateway. */
public final class Models {

    private Models() {
    }

    /** Usuario; el id es el de Keycloak (UUID). */
    public record UserView(String id, String login, String fullName, String email, Role role, String status) {
    }

    /** Datos que el autor captura en el formulario de pregunta (HU01). */
    public record QuestionForm(String type, String context, String directQuestion, List<String> distractors,
                               String correctAnswer, String justification, String bibliography,
                               String competence, String topic, String subtopic,
                               DifficultyLevel difficultyLevel, String authorLogin) {

        public QuestionForm withAuthor(String login) {
            return new QuestionForm(type, context, directQuestion, distractors, correctAnswer, justification,
                    bibliography, competence, topic, subtopic, difficultyLevel, login);
        }
    }

    /** Pregunta; version y revisable son del tercer corte (HU06). */
    public record QuestionView(String id, String type, String context, String directQuestion,
                               List<String> distractors, String correctAnswer, String justification,
                               String bibliography, String competence, String topic, String subtopic,
                               DifficultyLevel difficultyLevel, QuestionStatus status, String statusLabel,
                               String statusColor, boolean editable, String authorLogin, String createdAt,
                               int version, boolean revisable, String revisedAt) {

        public QuestionView {
            version = Math.max(version, 1);
        }
    }

    /** HU06: contenido archivado de una version anterior de la pregunta. */
    public record QuestionVersionView(String questionId, int version, String context, String directQuestion,
                                      List<String> distractors, String correctAnswer, String justification,
                                      String bibliography, String competence, String topic, String subtopic,
                                      DifficultyLevel difficultyLevel, QuestionStatus finalStatus,
                                      String finalStatusLabel, String archivedAt) {
    }

    public record PageView<T>(List<T> items, int page, int pageSize, long totalItems, int totalPages) {
    }

    /** HU04: pregunta Pendiente de revision con los revisores que ya tiene asignados. */
    public record PendingQuestionView(String questionId, String authorLogin, String directQuestion,
                                      String submittedAt, List<String> assignedReviewers, int round,
                                      int questionVersion, String status, String statusLabel) {

        public PendingQuestionView {
            assignedReviewers = assignedReviewers == null ? List.of() : List.copyOf(assignedReviewers);
        }
    }

    /** Asignacion de un revisor; active = todavia puede evaluarla (ronda vigente sin veredicto). */
    public record AssignmentView(String questionId, String reviewerLogin, String assignedBy, String assignedAt,
                                 int round, int questionVersion, String roundStatus, boolean active) {
    }

    public record EvaluationView(String id, String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                 String observations, String evaluatedAt, int round, int questionVersion) {
    }

    /** HU06: una ronda de revision (una por version de la pregunta). */
    public record RoundView(int round, int questionVersion, String directQuestion, String submittedAt,
                            String status, String statusLabel, List<AssignmentView> reviewers,
                            EvaluationView evaluation) {

        public RoundView {
            reviewers = reviewers == null ? List.of() : List.copyOf(reviewers);
        }
    }

    /** HU06: historial completo de revisiones de una pregunta. */
    public record ReviewHistoryView(String questionId, String authorLogin, int currentRound,
                                    int currentQuestionVersion, String currentStatus, String currentStatusLabel,
                                    List<RoundView> rounds) {

        public ReviewHistoryView {
            rounds = rounds == null ? List.of() : List.copyOf(rounds);
        }
    }
}
