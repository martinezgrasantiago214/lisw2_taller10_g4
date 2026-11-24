package co.unicauca.saberpro.question.domain.state;

import co.unicauca.saberpro.common.contract.QuestionStatus;

/**
 * La pregunta fue rechazada por un revisor (HU05); el autor lee las
 * observaciones y puede sacar una version mejorada (HU06), que vuelve a
 * Borrador.
 */
public class RechazadaState extends AbstractQuestionState {

    @Override
    public QuestionStatus status() {
        return QuestionStatus.RECHAZADA;
    }

    @Override
    public boolean isRevisable() {
        return true;
    }

    @Override
    public QuestionState startRevision() {
        return new BorradorState();
    }
}
