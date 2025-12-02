package com.example.esg.repo;

import com.example.esg.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyRepo extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {
}

