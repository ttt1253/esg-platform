package com.example.esg.repo;

import com.example.esg.domain.EsgScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface EsgScoreRepo extends JpaRepository<EsgScore, Long> {

    List<EsgScore> findByCompanyIdOrderByYearAsc(Long companyId);

    Optional<EsgScore> findByCompanyIdAndYear(Long companyId, Integer year);

    @Query("SELECT AVG(e.overall) FROM EsgScore e")
    Double findAverageOverallScore();

    EsgScore findFirstByOrderByOverallDesc();
}
