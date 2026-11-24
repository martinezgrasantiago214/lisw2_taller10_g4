package co.unicauca.saberpro.notification.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.QuestionResubmittedForReviewEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.notification.channel.NotificationChannel;
import co.unicauca.saberpro.notification.directory.UserDirectory;
import co.unicauca.saberpro.notification.domain.DeliveryResult;
import co.unicauca.saberpro.notification.domain.Notification;
import co.unicauca.saberpro.notification.domain.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Envia las notificaciones que disparan los eventos. Intenta primero el canal
 * principal (SMTP) y, si falla, deja el mensaje en el canal de respaldo
 * (bandeja de salida en archivo), como hacia el primer corte.
 */
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationChannel primary;
    private final NotificationChannel fallback;
    private final UserDirectory users;

    public NotificationService(NotificationChannel primary, NotificationChannel fallback, UserDirectory users) {
        this.primary = primary;
        this.fallback = fallback;
        this.users = users;
    }

    /** Entrega una notificacion por el canal principal o, si falla, por el de respaldo. */
    public DeliveryResult deliver(Notification notification) {
        DeliveryResult result = primary.send(notification);
        if (result.delivered() || fallback == null) {
            return result;
        }
        DeliveryResult fallbackResult = fallback.send(notification);
        return new DeliveryResult(fallbackResult.delivered(), fallbackResult.channel(),
                fallbackResult.detail() + " (" + primary.name() + ": " + result.detail() + ")");
    }

    /**
     * HU04 (PSP-34): avisa por correo a cada revisor asignado. Si no se
     * encuentra a un revisor o falla su envio, se intenta igual con los demas.
     *
     * @return el resultado del envio a cada revisor encontrado.
     */
    public List<DeliveryResult> onReviewersAssigned(ReviewersAssignedEvent event) {
        List<String> logins = event.reviewerLogins() == null ? List.of() : event.reviewerLogins();
        List<DeliveryResult> results = new ArrayList<>();
        for (String login : new LinkedHashSet<>(logins)) {
            Optional<Recipient> reviewer = findQuietly(login);
            if (reviewer.isEmpty()) {
                LOGGER.warn("No se encontro al revisor {} para avisarle la asignacion de la pregunta {}",
                        login, event.questionId());
                continue;
            }
            results.add(safeDeliver(assignmentNotice(reviewer.get(), event)));
        }
        return results;
    }

    private Optional<Recipient> findQuietly(String login) {
        try {
            return users.findByLogin(login);
        } catch (RuntimeException ex) {
            LOGGER.error("Error al consultar al usuario {}", login, ex);
            return Optional.empty();
        }
    }

    /**
     * HU05 (PSP-49): avisa al autor y a los administradores del resultado de
     * la evaluacion, con el veredicto y las observaciones del revisor.
     *
     * Cada destinatario recibe un solo correo, aunque el autor tambien sea
     * administrador. Si un envio falla, se intenta igual con los demas.
     *
     * @return el resultado del envio a cada destinatario encontrado.
     */
    public List<DeliveryResult> onQuestionEvaluated(QuestionEvaluatedEvent event) {
        Map<String, Recipient> recipients = new LinkedHashMap<>();
        users.findByLogin(event.authorLogin()).ifPresentOrElse(
                author -> recipients.put(author.login(), author),
                () -> LOGGER.warn("No se encontro al autor {} para avisarle la evaluacion de la pregunta {}",
                        event.authorLogin(), event.questionId()));
        for (Recipient admin : users.findAdministrators()) {
            recipients.putIfAbsent(admin.login(), admin);
        }
        if (recipients.isEmpty()) {
            LOGGER.warn("Nadie a quien avisar la evaluacion de la pregunta {}", event.questionId());
        }

        List<DeliveryResult> results = new ArrayList<>();
        for (Recipient recipient : recipients.values()) {
            boolean isAuthor = recipient.login().equals(event.authorLogin());
            results.add(safeDeliver(evaluationNotice(recipient, isAuthor, event)));
        }
        return results;
    }

    /**
     * HU06: llego a revision una version mejorada de una pregunta rechazada.
     * Se avisa a los implicados, cada uno con un solo correo:
     * <ul>
     *   <li>los administradores, para que asignen revisores a la ronda nueva;</li>
     *   <li>los revisores de la ronda anterior, para que sepan que se atendieron sus observaciones;</li>
     *   <li>el autor, como confirmacion.</li>
     * </ul>
     */
    public List<DeliveryResult> onQuestionResubmitted(QuestionResubmittedForReviewEvent event) {
        Map<String, Recipient> recipients = new LinkedHashMap<>();
        for (Recipient admin : users.findAdministrators()) {
            recipients.putIfAbsent(admin.login(), admin);
        }
        for (String reviewer : event.previousReviewers()) {
            findQuietly(reviewer).ifPresent(r -> recipients.putIfAbsent(r.login(), r));
        }
        findQuietly(event.authorLogin()).ifPresent(r -> recipients.putIfAbsent(r.login(), r));
        if (recipients.isEmpty()) {
            LOGGER.warn("Nadie a quien avisar la nueva version de la pregunta {}", event.questionId());
        }

        List<DeliveryResult> results = new ArrayList<>();
        for (Recipient recipient : recipients.values()) {
            results.add(safeDeliver(resubmissionNotice(recipient, event)));
        }
        return results;
    }

    private DeliveryResult safeDeliver(Notification notification) {
        try {
            DeliveryResult result = deliver(notification);
            if (result.delivered()) {
                LOGGER.info("Notificacion enviada a {} por {}", notification.recipientEmail(), result.channel());
            } else {
                LOGGER.warn("No se pudo notificar a {}: {}", notification.recipientEmail(), result.detail());
            }
            return result;
        } catch (RuntimeException ex) {
            LOGGER.error("Error al notificar a {}", notification.recipientEmail(), ex);
            return DeliveryResult.failed("ERROR", ex.getMessage());
        }
    }

    private static Notification assignmentNotice(Recipient reviewer, ReviewersAssignedEvent event) {
        String question = event.directQuestion() != null && !event.directQuestion().isBlank()
                ? event.directQuestion() : event.questionId();

        StringBuilder body = new StringBuilder();
        body.append("Hola ").append(reviewer.fullName()).append(",\n\n");
        body.append("El administrador");
        if (event.assignedBy() != null && !event.assignedBy().isBlank()) {
            body.append(" (").append(event.assignedBy()).append(')');
        }
        body.append(" le asigno una pregunta para revisar.\n\n");
        body.append("Pregunta: ").append(question).append('\n');
        body.append("Identificador: ").append(event.questionId()).append("\n\n");
        body.append("Ingrese al Banco de Preguntas Saber PRO, en \"Preguntas asignadas\", para aprobarla o "
                + "rechazarla con sus observaciones.\n\n");
        body.append("Banco de Preguntas Saber PRO");

        return new Notification(reviewer.email(), reviewer.fullName(),
                "Nueva pregunta para revisar: " + abbreviate(question), body.toString());
    }

    private static Notification evaluationNotice(Recipient recipient, boolean isAuthor, QuestionEvaluatedEvent event) {
        String verdict = event.verdict().getLabel();
        String question = event.directQuestion() != null && !event.directQuestion().isBlank()
                ? event.directQuestion() : event.questionId();
        String observations = event.observations() == null || event.observations().isBlank()
                ? "(el revisor no dejo observaciones)" : event.observations();

        StringBuilder body = new StringBuilder();
        body.append("Hola ").append(recipient.fullName()).append(",\n\n");
        body.append(isAuthor ? "Su pregunta fue evaluada" : "Se evaluo una pregunta del banco")
                .append(" y quedo en estado ").append(verdict).append(".\n\n");
        body.append("Pregunta: ").append(question).append('\n');
        body.append("Identificador: ").append(event.questionId()).append('\n');
        if (!isAuthor) {
            body.append("Autor: ").append(event.authorLogin()).append('\n');
        }
        body.append("Revisor: ").append(event.reviewerLogin()).append("\n\n");
        body.append("Observaciones del revisor:\n").append(observations).append("\n\n");
        if (event.questionVersion() > 1) {
            body.append("Version evaluada: ").append(event.questionVersion()).append("\n\n");
        }
        if (isAuthor && event.verdict() == EvaluationVerdict.RECHAZADA) {
            body.append("En \"Mis preguntas\" > \"Historial de revisiones\" puede leer las observaciones y "
                    + "crear una version mejorada de la pregunta para enviarla otra vez a revision.\n\n");
        }
        body.append("Banco de Preguntas Saber PRO");

        return new Notification(recipient.email(), recipient.fullName(), "Pregunta " + verdict + ": " + abbreviate(question),
                body.toString());
    }

    private static Notification resubmissionNotice(Recipient recipient, QuestionResubmittedForReviewEvent event) {
        String question = event.directQuestion() != null && !event.directQuestion().isBlank()
                ? event.directQuestion() : event.questionId();
        boolean isAuthor = recipient.login().equals(event.authorLogin());
        boolean isAdmin = "ADMINISTRADOR".equals(recipient.role());
        boolean wasReviewer = event.previousReviewers().contains(recipient.login());

        StringBuilder body = new StringBuilder();
        body.append("Hola ").append(recipient.fullName()).append(",\n\n");
        if (isAuthor) {
            body.append("Su version mejorada (version ").append(event.questionVersion())
                    .append(") quedo Pendiente de revision. Un administrador le asignara revisores.\n\n");
        } else {
            body.append("El autor ").append(event.authorLogin()).append(" envio una version mejorada (version ")
                    .append(event.questionVersion()).append(") de una pregunta que habia sido rechazada.\n\n");
        }
        body.append("Pregunta: ").append(question).append('\n');
        body.append("Identificador: ").append(event.questionId()).append('\n');
        body.append("Ronda de revision: ").append(event.round()).append("\n\n");
        if (event.previousObservations() != null && !event.previousObservations().isBlank()) {
            body.append("Observaciones de la ronda anterior:\n").append(event.previousObservations()).append("\n\n");
        }
        if (isAdmin && !isAuthor) {
            body.append("Ingrese a \"Asignar revisores\" para asignar de 1 a 3 revisores a esta version.\n\n");
        } else if (wasReviewer) {
            body.append("Usted reviso la version anterior; el administrador puede asignarle tambien esta.\n\n");
        }
        body.append("Banco de Preguntas Saber PRO");

        return new Notification(recipient.email(), recipient.fullName(),
                "Nueva version para revisar (v" + event.questionVersion() + "): " + abbreviate(question),
                body.toString());
    }

    private static String abbreviate(String text) {
        return text.length() <= 60 ? text : text.substring(0, 57) + "...";
    }
}
