package co.unicauca.saberpro.review.infrastructure.adapter.in.web;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.security.AuthenticatedUser;
import co.unicauca.saberpro.common.security.CurrentUserProvider;
import co.unicauca.saberpro.common.web.ForbiddenOperationException;
import co.unicauca.saberpro.review.application.port.in.AssignReviewersUseCase;
import co.unicauca.saberpro.review.application.port.in.AssignReviewersUseCase.AssignReviewersCommand;
import co.unicauca.saberpro.review.application.port.in.EvaluateQuestionUseCase;
import co.unicauca.saberpro.review.application.port.in.EvaluateQuestionUseCase.EvaluateQuestionCommand;
import co.unicauca.saberpro.review.application.port.in.ReviewQueries;
import co.unicauca.saberpro.review.domain.model.Evaluation;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewRound;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.AssignReviewersInput;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.AssignmentResponse;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.EvaluationInput;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.EvaluationResponse;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.PendingQuestionResponse;
import co.unicauca.saberpro.review.infrastructure.adapter.in.web.ReviewDtos.ReviewHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada (REST) de la revision por pares: HU04, HU05 y HU06.
 *
 * Solo traduce HTTP <-> casos de uso. La identidad (quien asigna, quien
 * evalua, quien consulta) sale del token JWT de Keycloak a traves de
 * {@link CurrentUserProvider}; la autorizacion por rol se declara con
 * {@code @PreAuthorize} y las reglas finas las decide el dominio.
 */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Revision por pares")
public class ReviewController {

    private final AssignReviewersUseCase assignReviewers;
    private final EvaluateQuestionUseCase evaluateQuestion;
    private final ReviewQueries queries;
    private final CurrentUserProvider currentUser;

    public ReviewController(AssignReviewersUseCase assignReviewers, EvaluateQuestionUseCase evaluateQuestion,
                            ReviewQueries queries, CurrentUserProvider currentUser) {
        this.assignReviewers = assignReviewers;
        this.evaluateQuestion = evaluateQuestion;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @GetMapping("/pending-questions")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "HU04: preguntas que esperan veredicto y que el administrador puede asignar [PSP-30]")
    public List<PendingQuestionResponse> pendingQuestions() {
        return queries.pendingQuestions().stream().map(PendingQuestionResponse::from).toList();
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "HU04: asigna entre 1 y 3 revisores a la ronda actual [PSP-31..34]")
    public List<AssignmentResponse> assign(@RequestBody AssignReviewersInput input) {
        String admin = currentUser.current().login();
        List<ReviewerAssignment> created = assignReviewers.assign(
                new AssignReviewersCommand(input.questionId(), input.reviewers(), admin));
        QuestionReview review = queries.history(input.questionId(), admin, true);
        ReviewRound round = review.currentRound();
        return created.stream().map(a -> AssignmentResponse.from(review.questionId(), round, a, true)).toList();
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('REVISOR', 'ADMINISTRADOR')")
    @Operation(summary = "HU05: preguntas asignadas a un revisor (por defecto, al usuario autenticado) [PSP-45]")
    public List<AssignmentResponse> assignedTo(@RequestParam(required = false) String reviewer) {
        Caller user = new Caller(currentUser.current());
        String target = reviewer == null || reviewer.isBlank() ? user.login() : reviewer.trim();
        if (!target.equals(user.login()) && !user.admin()) {
            throw new ForbiddenOperationException("Un revisor solo puede consultar sus propias asignaciones.");
        }
        return queries.assignedTo(target).stream().map(AssignmentResponse::from).toList();
    }

    @PostMapping("/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('REVISOR')")
    @Operation(summary = "HU05: el revisor autenticado aprueba o rechaza con observaciones [PSP-47..49]")
    public EvaluationResponse evaluate(@RequestBody EvaluationInput input) {
        String reviewer = currentUser.current().login();
        Evaluation evaluation = evaluateQuestion.evaluate(new EvaluateQuestionCommand(input.questionId(), reviewer,
                input.verdict(), input.observations()));
        QuestionReview review = queries.history(input.questionId(), reviewer, false);
        return EvaluationResponse.from(review.questionId(), review.currentRound(), evaluation);
    }

    @GetMapping("/evaluations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "HU05/HU06: evaluaciones y observaciones de todas las rondas de una pregunta [PSP-47]")
    public List<EvaluationResponse> evaluations(@RequestParam String questionId) {
        Caller user = new Caller(currentUser.current());
        return ReviewDtos.evaluationsOf(queries.history(questionId, user.login(), user.admin()));
    }

    @GetMapping("/history/{questionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "HU06: historial de revisiones (rondas, revisores, veredictos y observaciones)")
    public ReviewHistoryResponse history(@PathVariable String questionId) {
        Caller user = new Caller(currentUser.current());
        return ReviewHistoryResponse.from(queries.history(questionId, user.login(), user.admin()));
    }

    /** Login y si es administrador, tomados del token. */
    private record Caller(String login, boolean admin) {
        Caller(AuthenticatedUser user) {
            this(user.login(), user.hasRole(Role.ADMINISTRADOR));
        }
    }
}
