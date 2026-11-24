package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/**
 * Patron State (GoF, comportamiento): cada estado del ciclo de vida de una
 * pregunta es un objeto que sabe que transiciones permite. Una accion no
 * permitida en el estado actual lanza {@link InvalidStateTransitionException}.
 *
 * Cada transicion devuelve el estado siguiente, de modo que agregar un
 * estado nuevo (p. ej. "En correccion") no obliga a tocar los "if" de los
 * servicios: solo se crea otra clase.
 */
public interface QuestionState {

    QuestionStatus status();

    /** HU03: solo un Borrador se puede editar. */
    boolean isEditable();

    QuestionState submitForReview();

    QuestionState approve();

    QuestionState reject();

    QuestionState delete();

    /** HU06: solo una pregunta Rechazada admite una version mejorada. */
    boolean isRevisable();

    /** HU06: Rechazada -> Borrador (version mejorada); en otro estado no se permite. */
    QuestionState startRevision();
}
