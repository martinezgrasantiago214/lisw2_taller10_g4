package co.edu.unicauca.questionbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Saber PRO question bank. */
@SpringBootApplication
public class QuestionBankApplication {

    /** Creates the application class; Spring instantiates it. */
    public QuestionBankApplication() { }
    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(QuestionBankApplication.class, args);
    }
}
