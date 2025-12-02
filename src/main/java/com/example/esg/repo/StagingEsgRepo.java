package com.example.esg.repo;

import com.example.esg.domain.StagingEsg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Param import 필요
import java.util.List;
import java.util.Optional; // Optional import 필요

public interface StagingEsgRepo extends JpaRepository<StagingEsg, Long> {

    List<StagingEsg> findByCompanyNameContainingIgnoreCase(String keyword);

    @Query("SELECT DISTINCT s.companyName FROM StagingEsg s ORDER BY s.companyName")
    List<String> findAllCompanyNames();

    @Query("SELECT s.companyId FROM StagingEsg s WHERE s.companyName = :companyName ORDER BY s.id ASC LIMIT 1")
    Optional<Long> findCompanyIdByName(@Param("companyName") String companyName);

    @Query("SELECT MAX(s.companyId) FROM StagingEsg s")
    Optional<Long> findMaxCompanyId();

    List<StagingEsg> findAllByCompanyNameOrderByYearDesc(String companyName);
}