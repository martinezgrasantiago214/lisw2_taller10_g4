package co.unicauca.saberpro.question.access;

import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionVersion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de preguntas (DIP): el servicio de aplicacion
 * depende de esta interfaz, no de SQLite.
 */
public interface IQuestionRepository {

    /** @return true si la pregunta quedo guardada. */
    boolean save(Question question);

    /** @return true si la pregunta quedo actualizada (contenido y estado). */
    boolean update(Question question);

    Optional<Question> findById(String id);

    /** HU03: listado filtrado y paginado. */
    PagedResult<Question> search(QuestionFilterCriteria criteria);

    /** HU06: archiva el contenido de una version anterior. @return true si quedo guardada. */
    boolean saveVersion(QuestionVersion version);

    /** HU06: versiones archivadas de la pregunta, de la mas antigua a la mas reciente. */
    List<QuestionVersion> findVersions(String questionId);
}
