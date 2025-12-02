package com.example.esg.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name="company_financials",
        uniqueConstraints=@UniqueConstraint(columnNames={"company_id","year"}))
@Getter @Setter @NoArgsConstructor
public class CompanyFinancial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="company_id", nullable=false)
    private Company company;

    private Integer year;

    @Column(precision=15, scale=2) private BigDecimal revenue;
    @Column(precision=6,  scale=3) private BigDecimal profitMargin;
    @Column(precision=18, scale=2) private BigDecimal marketCap;
    @Column(precision=6,  scale=3) private BigDecimal growthRate;

    @Column(precision=18, scale=3) private BigDecimal carbonEmissions;
    @Column(precision=18, scale=3) private BigDecimal waterUsage;
    @Column(precision=18, scale=3) private BigDecimal energyConsumption;
}
