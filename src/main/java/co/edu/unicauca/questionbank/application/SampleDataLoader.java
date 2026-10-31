package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.domain.model.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

/**
 * Loads sample data at start-up so the H2 console can be explored. Enabled with
 * {@code app.seed-data=true}; disabled in tests. The question texts are in Spanish
 * because they are business data, not code.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class SampleDataLoader implements CommandLineRunner {
    private final CatalogService catalog;
    private final QuestionService questionService;
    private final ReviewService reviewService;
    private final QuizService quizService;

    /**
     * Creates a SampleDataLoader with the given values.
     *
     * @param catalog         catalog use cases
     * @param questionService question use cases
     * @param reviewService   review use cases
     * @param quizService     quiz use cases
     */
    public SampleDataLoader(CatalogService catalog, QuestionService questionService,
                            ReviewService reviewService, QuizService quizService) {
        this.catalog = catalog;
        this.questionService = questionService;
        this.reviewService = reviewService;
        this.quizService = quizService;
    }

    /** {@inheritDoc} */
    @Override
    public void run(String... args) {
        User author = catalog.registerUser("Ana Autora", "autora@unicauca.edu.co", Role.AUTHOR);
        catalog.registerUser("Rafael Revisor", "revisor@unicauca.edu.co", Role.REVIEWER);
        Topic percentages = catalog.createTopic("Porcentajes y proporciones", "Razonamiento cuantitativo");
        Topic argumentation = catalog.createTopic("Texto argumentativo", "Comunicación escrita");
        Competency interpret = catalog.createCompetency("RC-INT", "Interpretación y representación");
        Competency formulate = catalog.createCompetency("RC-FOR", "Formulación y ejecución");
        Competency position = catalog.createCompetency("CE-PLA", "Planteamiento de una postura argumentada");

        Question mc = questionService.createMultipleChoiceQuestion(
            "Un artículo cuesta $80.000 y tiene un descuento del 15 %. ¿Cuál es su precio final?",
            DifficultyLevel.LOW, author.getId(), percentages.getId(),
            List.of(new AnswerOption("A", "$68.000", true),
                    new AnswerOption("B", "$65.000", false),
                    new AnswerOption("C", "$12.000", false),
                    new AnswerOption("D", "$92.000", false),
                    new AnswerOption("E", "$70.000", false)),
            Set.of(interpret.getId(), formulate.getId()));
        questionService.submitForReview(mc.getId());
        reviewService.startReview(mc.getId());
        reviewService.approve(mc.getId());
        reviewService.publish(mc.getId());

        Question tf = questionService.createTrueFalseQuestion(
            "Un aumento del 10 % seguido de una disminución del 10 % deja el valor original.",
            DifficultyLevel.MEDIUM, author.getId(), percentages.getId(), false, Set.of(interpret.getId()));
        questionService.submitForReview(tf.getId());

        questionService.createOpenQuestion(
            "Escriba un texto argumentativo sobre el uso de la inteligencia artificial en la educación superior.",
            DifficultyLevel.MEDIUM, author.getId(), argumentation.getId(),
            "Postura clara, al menos dos argumentos sustentados y una conclusión coherente.", 300,
            Set.of(position.getId()));

        quizService.createQuiz("Simulacro 1", "Razonamiento cuantitativo", List.of(mc.getId()));
    }
}
