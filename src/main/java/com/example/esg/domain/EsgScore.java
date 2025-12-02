package com.example.esg.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "esg_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "year"}))
@Getter
@Setter
@NoArgsConstructor
public class EsgScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private Integer year;
    private BigDecimal eScore;
    private BigDecimal sScore;
    private BigDecimal gScore;
    private BigDecimal overall;
}
