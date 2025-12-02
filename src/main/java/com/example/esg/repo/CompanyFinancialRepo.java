package com.example.esg.repo;

import com.example.esg.domain.CompanyFinancial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyFinancialRepo extends JpaRepository<CompanyFinancial, Long> {

    Optional<CompanyFinancial> findByCompanyIdAndYear(Long companyId, Integer year);

    List<CompanyFinancial> findByCompanyIdOrderByYearAsc(Long companyId);
}
