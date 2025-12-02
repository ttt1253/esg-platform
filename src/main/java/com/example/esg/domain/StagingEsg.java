package com.example.esg.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "staging_esg_full")
@Getter @Setter
public class StagingEsg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "companyid")
    private Long companyId;

    @Column(name = "companyname")
    private String companyName;

    private String industry;
    private String region;
    private Integer year;

    @Column(name = "esg_overall")
    private BigDecimal esgOverall;

    @Column(name = "esg_environmental")
    private BigDecimal esgEnvironmental;

    @Column(name = "esg_social")
    private BigDecimal esgSocial;

    @Column(name = "esg_governance")
    private BigDecimal esgGovernance;

    private BigDecimal revenue;

    @Column(name = "profitmargin")
    private BigDecimal profitMargin;
}