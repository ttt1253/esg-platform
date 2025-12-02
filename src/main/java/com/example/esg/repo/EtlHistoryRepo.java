package com.example.esg.repo;

import com.example.esg.domain.EtlHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EtlHistoryRepo extends JpaRepository<EtlHistory, Long> {
    List<EtlHistory> findAllByOrderByCreatedAtDesc();
}