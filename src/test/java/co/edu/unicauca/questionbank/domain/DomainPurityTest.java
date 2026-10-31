package co.edu.unicauca.questionbank.domain;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Architecture test: the domain package must not depend on JPA or Spring.
 * Adding {@code @Entity} or a {@code jakarta.persistence} import to a domain class breaks it.
 */
class DomainPurityTest {
    private static final Path DOMAIN = Path.of("src/main/java/co/edu/unicauca/questionbank/domain");

    @Test
    void domainHasNoPersistenceOrFrameworkDependencies() throws IOException {
        assertTrue(Files.isDirectory(DOMAIN), "Domain package not found");
        List<Path> offenders;
        try (Stream<Path> files = Files.walk(DOMAIN)) {
            offenders = files.filter(p -> p.toString().endsWith(".java")).filter(DomainPurityTest::isContaminated).toList();
        }
        assertTrue(offenders.isEmpty(), "Domain classes with technology dependencies: " + offenders);
    }

    private static boolean isContaminated(Path file) {
        try {
            String source = Files.readString(file);
            return source.contains("jakarta.persistence") || source.contains("javax.persistence")
                || source.contains("org.springframework") || source.contains("@Entity");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
