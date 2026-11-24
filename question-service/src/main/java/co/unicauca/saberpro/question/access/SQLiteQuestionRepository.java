package co.unicauca.saberpro.question.access;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion de {@link IQuestionRepository} con SQLite: base de datos
 * PROPIA del question-service. Los distractores se guardan como texto
 * delimitado por "||". Los revisores asignados ya no estan aqui: pertenecen
 * al review-service.
 *
 * Metodos synchronized: se comparte una sola conexion JDBC entre los hilos de Tomcat.
 */
public class SQLiteQuestionRepository implements IQuestionRepository, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteQuestionRepository.class);
    private static final String DELIM = "\\|\\|";
    private static final String JOIN = "||";
    private static final String COLUMNS = "id, type, context, directQuestion, distractors, correctAnswer, "
            + "justification, bibliography, competence, topic, subtopic, difficultyLevel, status, "
            + "authorLogin, createdAt, version, revisedAt";

    private final String url;
    private Connection conn;

    public SQLiteQuestionRepository(String url) {
        this.url = url;
        initDatabase();
    }

    private Connection connect() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(url);
            try (Statement pragma = conn.createStatement()) {
                pragma.execute("PRAGMA busy_timeout = 5000;");
                pragma.execute("PRAGMA journal_mode = WAL;");
            }
        }
        return conn;
    }

    private synchronized void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS Question (\n"
                + "  id TEXT PRIMARY KEY,\n"
                + "  type TEXT NOT NULL,\n"
                + "  context TEXT,\n"
                + "  directQuestion TEXT,\n"
                + "  distractors TEXT,\n"
                + "  correctAnswer TEXT,\n"
                + "  justification TEXT,\n"
                + "  bibliography TEXT,\n"
                + "  competence TEXT,\n"
                + "  topic TEXT,\n"
                + "  subtopic TEXT,\n"
                + "  difficultyLevel TEXT,\n"
                + "  status TEXT NOT NULL,\n"
                + "  authorLogin TEXT,\n"
                + "  createdAt TEXT,\n"
                + "  version INTEGER NOT NULL DEFAULT 1,\n"
                + "  revisedAt TEXT\n"
                + ");";
        String versions = "CREATE TABLE IF NOT EXISTS QuestionVersion (\n"
                + "  questionId TEXT NOT NULL,\n"
                + "  version INTEGER NOT NULL,\n"
                + "  context TEXT,\n"
                + "  directQuestion TEXT,\n"
                + "  distractors TEXT,\n"
                + "  correctAnswer TEXT,\n"
                + "  justification TEXT,\n"
                + "  bibliography TEXT,\n"
                + "  competence TEXT,\n"
                + "  topic TEXT,\n"
                + "  subtopic TEXT,\n"
                + "  difficultyLevel TEXT,\n"
                + "  finalStatus TEXT,\n"
                + "  archivedAt TEXT,\n"
                + "  PRIMARY KEY (questionId, version)\n"
                + ");";
        try (Statement stmt = connect().createStatement()) {
            stmt.execute(sql);
            // Tercer corte (HU06): bases de datos del segundo corte no tienen estas columnas.
            addColumnIfMissing(stmt, "version", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfMissing(stmt, "revisedAt", "TEXT");
            stmt.execute(versions);
        } catch (SQLException ex) {
            LOGGER.error("No fue posible crear las tablas del question-service (URL: {})", url, ex);
        }
    }

    private void addColumnIfMissing(Statement stmt, String column, String definition) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(Question)")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        stmt.execute("ALTER TABLE Question ADD COLUMN " + column + " " + definition);
        LOGGER.info("Columna {} agregada a la tabla Question", column);
    }

    @Override
    public synchronized boolean save(Question q) {
        String sql = "INSERT INTO Question (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, q.getId());
            bindContent(p, q, 2);
            return p.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("No fue posible guardar la pregunta {}", q.getId(), ex);
            return false;
        }
    }

    @Override
    public synchronized boolean update(Question q) {
        String sql = "UPDATE Question SET type=?, context=?, directQuestion=?, distractors=?, correctAnswer=?, "
                + "justification=?, bibliography=?, competence=?, topic=?, subtopic=?, difficultyLevel=?, "
                + "status=?, authorLogin=?, createdAt=?, version=?, revisedAt=? WHERE id=?";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            bindContent(p, q, 1);
            p.setString(17, q.getId());
            return p.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("No fue posible actualizar la pregunta {}", q.getId(), ex);
            return false;
        }
    }

    @Override
    public synchronized Optional<Question> findById(String id) {
        try (PreparedStatement p = connect().prepareStatement("SELECT " + COLUMNS + " FROM Question WHERE id = ?")) {
            p.setString(1, id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible buscar la pregunta {}", id, ex);
        }
        return Optional.empty();
    }

    /** Filtra en SQL (autor, estado, tema, palabra clave) y pagina con LIMIT/OFFSET. */
    @Override
    public synchronized PagedResult<Question> search(QuestionFilterCriteria criteria) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (criteria.getAuthorLogin() != null && !criteria.getAuthorLogin().isBlank()) {
            where.append(" AND authorLogin = ?");
            params.add(criteria.getAuthorLogin());
        }
        if (criteria.getStatus() != null) {
            where.append(" AND status = ?");
            params.add(criteria.getStatus().name());
        }
        if (criteria.getTopic() != null && !criteria.getTopic().isBlank()) {
            where.append(" AND topic LIKE ?");
            params.add("%" + criteria.getTopic() + "%");
        }
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            where.append(" AND (directQuestion LIKE ? OR context LIKE ? OR topic LIKE ?)");
            for (int i = 0; i < 3; i++) {
                params.add("%" + criteria.getKeyword() + "%");
            }
        }

        int pageSize = criteria.getPageSize();
        int page = criteria.getPage();
        List<Question> items = new ArrayList<>();
        long total = 0;
        try {
            try (PreparedStatement count = connect().prepareStatement("SELECT COUNT(*) FROM Question" + where)) {
                bindParams(count, params);
                try (ResultSet rs = count.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0;
                }
            }
            try (PreparedStatement select = connect().prepareStatement("SELECT " + COLUMNS + " FROM Question"
                    + where + " ORDER BY createdAt DESC LIMIT ? OFFSET ?")) {
                bindParams(select, params);
                select.setInt(params.size() + 1, pageSize);
                select.setInt(params.size() + 2, (page - 1) * pageSize);
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible buscar preguntas (autor={}, estado={})",
                    criteria.getAuthorLogin(), criteria.getStatus(), ex);
        }
        return new PagedResult<>(items, page, pageSize, total);
    }

    private void bindParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }

    /** Asigna las columnas desde "type" hasta "revisedAt" empezando en la posicion indicada. */
    private void bindContent(PreparedStatement p, Question q, int start) throws SQLException {
        p.setString(start, q.getType());
        p.setString(start + 1, q.getContext());
        p.setString(start + 2, q.getDirectQuestion());
        p.setString(start + 3, String.join(JOIN, q.getDistractors()));
        p.setString(start + 4, q.getCorrectAnswer());
        p.setString(start + 5, q.getJustification());
        p.setString(start + 6, q.getBibliography());
        p.setString(start + 7, q.getCompetence());
        p.setString(start + 8, q.getTopic());
        p.setString(start + 9, q.getSubtopic());
        p.setString(start + 10, q.getDifficultyLevel() != null ? q.getDifficultyLevel().name() : null);
        p.setString(start + 11, q.getStatus().name());
        p.setString(start + 12, q.getAuthorLogin());
        p.setString(start + 13, q.getCreatedAt().toString());
        p.setInt(start + 14, q.getVersion());
        p.setString(start + 15, q.getRevisedAt() != null ? q.getRevisedAt().toString() : null);
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        String difficulty = rs.getString("difficultyLevel");
        String createdAt = rs.getString("createdAt");
        String revisedAt = rs.getString("revisedAt");
        return Question.builder()
                .id(rs.getString("id"))
                .type(rs.getString("type"))
                .context(rs.getString("context"))
                .directQuestion(rs.getString("directQuestion"))
                .distractors(splitOrEmpty(rs.getString("distractors")))
                .correctAnswer(rs.getString("correctAnswer"))
                .justification(rs.getString("justification"))
                .bibliography(rs.getString("bibliography"))
                .competence(rs.getString("competence"))
                .topic(rs.getString("topic"))
                .subtopic(rs.getString("subtopic"))
                .difficultyLevel(difficulty != null ? DifficultyLevel.valueOf(difficulty) : null)
                .status(QuestionStatus.valueOf(rs.getString("status")))
                .authorLogin(rs.getString("authorLogin"))
                .createdAt(createdAt != null ? LocalDateTime.parse(createdAt) : null)
                .version(rs.getInt("version"))
                .revisedAt(revisedAt != null ? LocalDateTime.parse(revisedAt) : null)
                .build();
    }

    // ---- HU06: versiones archivadas ----

    @Override
    public synchronized boolean saveVersion(QuestionVersion v) {
        String sql = "INSERT OR REPLACE INTO QuestionVersion (questionId, version, context, directQuestion, distractors, "
                + "correctAnswer, justification, bibliography, competence, topic, subtopic, difficultyLevel, "
                + "finalStatus, archivedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, v.questionId());
            p.setInt(2, v.version());
            p.setString(3, v.context());
            p.setString(4, v.directQuestion());
            p.setString(5, String.join(JOIN, v.distractors()));
            p.setString(6, v.correctAnswer());
            p.setString(7, v.justification());
            p.setString(8, v.bibliography());
            p.setString(9, v.competence());
            p.setString(10, v.topic());
            p.setString(11, v.subtopic());
            p.setString(12, v.difficultyLevel() != null ? v.difficultyLevel().name() : null);
            p.setString(13, v.finalStatus() != null ? v.finalStatus().name() : null);
            p.setString(14, v.archivedAt() != null ? v.archivedAt().toString() : null);
            return p.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("No fue posible archivar la version {} de la pregunta {}", v.version(), v.questionId(), ex);
            return false;
        }
    }

    @Override
    public synchronized List<QuestionVersion> findVersions(String questionId) {
        String sql = "SELECT * FROM QuestionVersion WHERE questionId = ? ORDER BY version";
        List<QuestionVersion> result = new ArrayList<>();
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, questionId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    String difficulty = rs.getString("difficultyLevel");
                    String status = rs.getString("finalStatus");
                    String archivedAt = rs.getString("archivedAt");
                    result.add(new QuestionVersion(rs.getString("questionId"), rs.getInt("version"),
                            rs.getString("context"), rs.getString("directQuestion"),
                            splitOrEmpty(rs.getString("distractors")), rs.getString("correctAnswer"),
                            rs.getString("justification"), rs.getString("bibliography"), rs.getString("competence"),
                            rs.getString("topic"), rs.getString("subtopic"),
                            difficulty != null ? DifficultyLevel.valueOf(difficulty) : null,
                            status != null ? QuestionStatus.valueOf(status) : null,
                            archivedAt != null ? LocalDateTime.parse(archivedAt) : null));
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible consultar las versiones de la pregunta {}", questionId, ex);
        }
        return result;
    }

    private List<String> splitOrEmpty(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(DELIM)));
    }

    /** Cierra la conexion (Spring lo invoca al apagar el servicio). */
    @Override
    public synchronized void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            LOGGER.warn("Error cerrando la conexion", ex);
        }
    }
}
