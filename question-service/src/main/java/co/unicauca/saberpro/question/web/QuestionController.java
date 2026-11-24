package co.unicauca.saberpro.question.web;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.security.AuthenticatedUser;
import co.unicauca.saberpro.common.security.CurrentUserProvider;
import co.unicauca.saberpro.common.web.ForbiddenOperationException;
import co.unicauca.saberpro.question.access.QuestionFilterCriteria;
import co.unicauca.saberpro.question.application.QuestionApplicationService;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.web.QuestionDtos.PageResponse;
import co.unicauca.saberpro.question.web.QuestionDtos.QuestionInput;
import co.unicauca.saberpro.question.web.QuestionDtos.QuestionResponse;
import co.unicauca.saberpro.question.web.QuestionDtos.QuestionTypeResponse;
import co.unicauca.saberpro.question.web.QuestionDtos.QuestionVersionResponse;
import co.unicauca.saberpro.question.web.QuestionDtos.SubmitReviewInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de preguntas (HU01, HU02, HU03 y HU06).
 *
 * Tercer corte: el autor ya no se toma del cuerpo JSON sino del token JWT de
 * Keycloak ({@link CurrentUserProvider}); asi nadie puede crear, editar o
 * enviar preguntas a nombre de otro. {@code @PreAuthorize} exige el rol de
 * cada operacion y el servicio de aplicacion sigue verificando que el
 * usuario sea el autor.
 */
@RestController
@RequestMapping("/api/questions")
@Tag(name = "Preguntas")
public class QuestionController {

    private final QuestionApplicationService service;
    private final CurrentUserProvider currentUser;

    public QuestionController(QuestionApplicationService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AUTOR_PREGUNTAS')")
    @Operation(summary = "HU01: crea una pregunta a nombre del usuario autenticado (queda en Borrador) [PSP-16/17/18]")
    public QuestionResponse create(@RequestBody QuestionInput input) {
        return QuestionResponse.from(service.create(input.toRequest(currentUser.current().login())));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "HU03: lista preguntas con filtros y paginacion; un autor solo ve las suyas [PSP-24..27]")
    public PageResponse<QuestionResponse> search(@RequestParam(required = false) String author,
                                                 @RequestParam(required = false) QuestionStatus status,
                                                 @RequestParam(required = false) String topic,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        AuthenticatedUser user = currentUser.current();
        String effectiveAuthor = user.isAdministrator() ? author : user.login();
        QuestionFilterCriteria criteria = new QuestionFilterCriteria()
                .setAuthorLogin(effectiveAuthor).setStatus(status).setTopic(topic).setKeyword(keyword)
                .setPage(page).setPageSize(size);
        return PageResponse.from(service.search(criteria));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulta una pregunta por id (su autor, administradores y revisores)")
    public QuestionResponse findById(@PathVariable String id) {
        return QuestionResponse.from(readable(service.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('AUTOR_PREGUNTAS')")
    @Operation(summary = "HU03: edita una pregunta propia en Borrador [PSP-28]")
    public QuestionResponse update(@PathVariable String id, @RequestBody QuestionInput input) {
        return QuestionResponse.from(service.update(id, input.toRequest(currentUser.current().login())));
    }

    @PatchMapping("/{id}/submit-review")
    @PreAuthorize("hasRole('AUTOR_PREGUNTAS')")
    @Operation(summary = "HU02: Borrador -> Pendiente de revision [PSP-21]")
    public QuestionResponse submitForReview(@PathVariable String id,
                                            @RequestBody(required = false) SubmitReviewInput ignored) {
        return QuestionResponse.from(service.submitForReview(id, currentUser.current().login()));
    }

    @PostMapping("/{id}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AUTOR_PREGUNTAS')")
    @Operation(summary = "HU06: crea la version mejorada de una pregunta Rechazada (vuelve a Borrador, version + 1)")
    public QuestionResponse revise(@PathVariable String id, @RequestBody QuestionInput input) {
        String author = currentUser.current().login();
        return QuestionResponse.from(service.revise(id, input.toRequest(author), author));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "HU06: versiones anteriores archivadas de la pregunta, de la mas antigua a la mas reciente")
    public List<QuestionVersionResponse> versions(@PathVariable String id) {
        readable(service.findById(id));
        return service.versions(id).stream().map(QuestionVersionResponse::from).toList();
    }

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tipos de pregunta disponibles (fabricas cargadas por reflexion)")
    public List<QuestionTypeResponse> types() {
        return service.availableTypes().stream().map(QuestionTypeResponse::from).toList();
    }

    /** Una pregunta la leen su autor, los administradores y los revisores (HU05: la leen antes de evaluar). */
    private Question readable(Question question) {
        AuthenticatedUser user = currentUser.current();
        if (question.isAuthoredBy(user.login()) || user.isAdministrator() || user.hasRole(Role.REVISOR)) {
            return question;
        }
        throw new ForbiddenOperationException("Solo el autor, los administradores y los revisores pueden ver esta pregunta.");
    }
}
