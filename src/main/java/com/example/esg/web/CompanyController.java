package com.example.esg.web;

import com.example.esg.domain.Company;
import com.example.esg.domain.EsgScore;
import com.example.esg.domain.CompanyFinancial;
import com.example.esg.repo.CompanyRepo;
import com.example.esg.repo.EsgScoreRepo;
import com.example.esg.repo.CompanyFinancialRepo;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepo companyRepo;
    private final EsgScoreRepo esgRepo;
    private final CompanyFinancialRepo companyFinancialRepo;

    //1. 메인 페이지: http://localhost:8080/
    @GetMapping("/")
    public String list(
            Model model,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> industries,
            @RequestParam(required = false) List<String> regions,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 50) size = 50;

        Sort sort = "desc".equalsIgnoreCase(dir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Company> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"));
            }
            if (industries != null && !industries.isEmpty()) {
                predicates.add(root.get("industry").in(industries));
            }
            if (regions != null && !regions.isEmpty()) {
                predicates.add(root.get("region").in(regions));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Company> result = companyRepo.findAll(spec, pageable);

        long totalCompanies = companyRepo.count();

        Double avgScoreRaw = esgRepo.findAverageOverallScore();
        String avgScore = (avgScoreRaw != null) ? String.format("%.1f", avgScoreRaw) : "0.0";

        EsgScore topEsg = esgRepo.findFirstByOrderByOverallDesc();
        String topCompany = "-";
        Object topScoreVal = 0;

        if (topEsg != null) {
            topCompany = topEsg.getCompany().getName();
            topScoreVal = topEsg.getOverall();
        }

        model.addAttribute("totalCompanies", totalCompanies);
        model.addAttribute("avgScore", avgScore);
        model.addAttribute("topCompany", topCompany);
        model.addAttribute("topScoreVal", topScoreVal);

        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("industryList", List.of(
                "Consumer Goods", "Energy", "Finance", "Healthcare",
                "Manufacturing", "Retail", "Technology", "Transportation", "Utilities"
        ));
        model.addAttribute("regionList", List.of(
                "Africa", "Asia", "Europe", "Latin America", "Middle East",
                "North America", "Oceania"
        ));
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);

        return "list";
    }

    @GetMapping("/companies/{id}")
    public String detail(@PathVariable Long id, Model model) {

        Company company = companyRepo.findById(id).orElseThrow();

        List<EsgScore> scores =
                esgRepo.findByCompanyIdOrderByYearAsc(id);

        List<CompanyFinancial> financials =
                companyFinancialRepo.findByCompanyIdOrderByYearAsc(id);

        // ⭐ 비교용 기업 리스트 전달
        List<Company> allCompanies = companyRepo.findAll();

        model.addAttribute("company", company);
        model.addAttribute("scores", scores);
        model.addAttribute("financials", financials);
        model.addAttribute("allCompanies", allCompanies);

        return "detail";
    }

    @GetMapping("/companies/compare")
    public String compare(
            @RequestParam Long base,
            @RequestParam Long target,
            Model model
    ) {

        Company c1 = companyRepo.findById(base).orElseThrow();
        Company c2 = companyRepo.findById(target).orElseThrow();

        List<EsgScore> s1 = esgRepo.findByCompanyIdOrderByYearAsc(base);
        List<EsgScore> s2 = esgRepo.findByCompanyIdOrderByYearAsc(target);

        model.addAttribute("c1", c1);
        model.addAttribute("c2", c2);

        model.addAttribute("s1", s1);
        model.addAttribute("s2", s2);

        return "compare";
    }

}
