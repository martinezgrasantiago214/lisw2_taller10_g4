package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.application.CatalogService;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end tests: HTTP, services, JPA repositories and H2 working together. */
@SpringBootTest(properties = {"app.seed-data=false", "spring.datasource.url=jdbc:h2:mem:${random.uuid};DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class QuestionApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired ObjectMapper json;

    private long createQuestion(User author, Topic topic, Competency competency) throws Exception {
        String body = """
            {"statement":"10%% of 50?","difficulty":"LOW","authorId":%d,"topicId":%d,"competencyIds":[%d],
             "options":[{"label":"A","text":"5","correct":true},{"label":"B","text":"10","correct":false},
                        {"label":"C","text":"0.5","correct":false},{"label":"D","text":"50","correct":false},
                        {"label":"E","text":"500","correct":false}]}
            """.formatted(author.getId(), topic.getId(), competency.getId());
        String created = mvc.perform(post("/api/questions/multiple-choice").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("MULTIPLE_CHOICE"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(created).get("id").asLong();
    }

    @Test
    void questionGoesThroughItsLifecycleAndIntoAQuiz() throws Exception {
        User author = catalog.registerUser("Luis", "luis.api@unicauca.edu.co", Role.AUTHOR);
        Topic topic = catalog.createTopic("Ratios API", "Quantitative reasoning");
        Competency competency = catalog.createCompetency("API-INT", "Interpretation");
        long id = createQuestion(author, topic, competency);

        mvc.perform(post("/api/quizzes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Mock\",\"questionIds\":[" + id + "]}"))
            .andExpect(status().isConflict());

        mvc.perform(post("/api/questions/{id}/submit", id)).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(post("/api/questions/{id}/start-review", id)).andExpect(jsonPath("$.status").value("IN_REVIEW"));
        mvc.perform(post("/api/questions/{id}/approve", id)).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/questions/{id}/publish", id)).andExpect(jsonPath("$.status").value("PUBLISHED"));

        mvc.perform(post("/api/quizzes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Mock\",\"questionIds\":[" + id + "]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.questionIds[0]").value(id));

        mvc.perform(get("/api/questions/published").param("competencyId", String.valueOf(competency.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void invalidRequestsReturnTheRightStatus() throws Exception {
        User reviewer = catalog.registerUser("Rafa", "rafa.api@unicauca.edu.co", Role.REVIEWER);
        Topic topic = catalog.createTopic("Logic API", "Quantitative reasoning");
        String body = "{\"statement\":\"x\",\"difficulty\":\"LOW\",\"authorId\":" + reviewer.getId()
            + ",\"topicId\":" + topic.getId() + ",\"expectedValue\":true}";

        mvc.perform(post("/api/questions/true-false").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/questions/{id}", 987654)).andExpect(status().isNotFound());
        mvc.perform(post("/api/questions/{id}/approve", 987654)).andExpect(status().isNotFound());
    }
}
