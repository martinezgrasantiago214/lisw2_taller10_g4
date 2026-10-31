package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.infrastructure.jpa.record.CompetencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data interface used only by {@code CompetencyRepositoryJpa}. */
public interface SpringCompetencyDataRepository extends JpaRepository<CompetencyRecord, Long> { }
