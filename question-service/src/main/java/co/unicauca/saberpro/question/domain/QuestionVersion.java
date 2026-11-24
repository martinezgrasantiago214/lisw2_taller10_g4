package co.unicauca.saberpro.question.domain;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU06: contenido archivado de una version anterior de la pregunta (objeto
 * de valor inmutable). Cuando el autor saca una version mejorada de una
 * pregunta rechazada, la version rechazada se guarda asi, para que el
 * historial muestre que se reviso en cada ronda.
 */
public record QuestionVersion(String questionId, int version, String context, String directQuestion,
                              List<String> distractors, String correctAnswer, String justification,
                              String bibliography, String competence, String topic, String subtopic,
                              DifficultyLevel difficultyLevel, QuestionStatus finalStatus,
                              LocalDateTime archivedAt) {

    public QuestionVersion {
        distractors = distractors == null ? List.of() : List.copyOf(distractors);
    }
}
