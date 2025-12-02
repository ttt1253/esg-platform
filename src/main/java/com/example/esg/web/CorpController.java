package com.example.esg.web;

import com.example.esg.domain.*;
import com.example.esg.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/corp")
@RequiredArgsConstructor
public class CorpController {

    private final UserRepo userRepo;
    private final StagingEsgRepo stagingRepo;
    private final CompanyRepo companyRepo;
    private final EsgScoreRepo esgScoreRepo;
    private final CompanyFinancialRepo financialRepo;

    @GetMapping("/data")
    public String corpDataList(Model model, Principal principal) {
        SiteUser user = userRepo.findByUsername(principal.getName()).orElseThrow();

        if (user.getCompany() == null) {
            model.addAttribute("msg", "소속된 회사가 없습니다. 관리자에게 문의하세요.");
            return "error/403";
        }

        String myCompanyName = user.getCompany().getName();

        List<StagingEsg> list = stagingRepo.findAllByCompanyNameOrderByYearDesc(myCompanyName);

        model.addAttribute("list", list);
        model.addAttribute("companyName", myCompanyName);

        return "corp/data_list";
    }

    @PostMapping("/data/update")
    public String corpDataUpdate(@RequestParam(required = false) Long id,
                                 @ModelAttribute StagingEsg formData,
                                 Principal principal) {

        SiteUser user = userRepo.findByUsername(principal.getName()).orElseThrow();
        if (user.getCompany() == null) {
            return "redirect:/";
        }
        String myCompanyName = user.getCompany().getName();

        StagingEsg data;

        if (id != null) {
            data = stagingRepo.findById(id).orElse(new StagingEsg());

            if (!data.getCompanyName().equals(myCompanyName)) {
                return "redirect:/corp/data?error=unauthorized";
            }
        }
        else {
            data = new StagingEsg();
        }

        data.setCompanyName(myCompanyName);

        data.setYear(formData.getYear());
        data.setIndustry(formData.getIndustry());
        data.setRegion(formData.getRegion());

        data.setEsgOverall(formData.getEsgOverall());
        data.setEsgEnvironmental(formData.getEsgEnvironmental());
        data.setEsgSocial(formData.getEsgSocial());
        data.setEsgGovernance(formData.getEsgGovernance());

        data.setRevenue(formData.getRevenue());
        data.setProfitMargin(formData.getProfitMargin());

        Long existingId = stagingRepo.findCompanyIdByName(myCompanyName).orElse(null);

        if (existingId != null) {
            data.setCompanyId(existingId);
        } else {
            Long maxId = stagingRepo.findMaxCompanyId().orElse(0L);
            data.setCompanyId(maxId + 1);
        }

        stagingRepo.save(data);

        syncToRealTables(data);

        return "redirect:/corp/data";
    }

    @GetMapping("/data/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String corpDataDelete(@PathVariable Long id, Principal principal) {

        SiteUser user = userRepo.findByUsername(principal.getName()).orElseThrow();
        String myCompanyName = user.getCompany().getName();

        StagingEsg target = stagingRepo.findById(id).orElse(null);

        if (target != null && target.getCompanyName().equals(myCompanyName)) {

            companyRepo.findAll().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(target.getCompanyName()))
                    .findFirst()
                    .ifPresent(company -> {
                        esgScoreRepo.findByCompanyIdAndYear(company.getId(), target.getYear())
                                .ifPresent(esgScoreRepo::delete);

                        financialRepo.findByCompanyIdAndYear(company.getId(), target.getYear())
                                .ifPresent(financialRepo::delete);
                    });

            stagingRepo.deleteById(id);
        }

        return "redirect:/corp/data";
    }

    private void syncToRealTables(StagingEsg s) {
        Company company = companyRepo.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(s.getCompanyName()))
                .findFirst()
                .orElseGet(() -> {
                    Company newComp = new Company();
                    newComp.setName(s.getCompanyName());
                    return newComp;
                });

        company.setIndustry(s.getIndustry());
        company.setRegion(s.getRegion());
        companyRepo.save(company);

        EsgScore esg = esgScoreRepo.findByCompanyIdAndYear(company.getId(), s.getYear())
                .orElseGet(() -> {
                    EsgScore newScore = new EsgScore();
                    newScore.setCompany(company);
                    newScore.setYear(s.getYear());
                    return newScore;
                });

        esg.setOverall(s.getEsgOverall());
        esg.setEScore(s.getEsgEnvironmental());
        esg.setSScore(s.getEsgSocial());
        esg.setGScore(s.getEsgGovernance());
        esgScoreRepo.save(esg);

        CompanyFinancial fin = financialRepo.findByCompanyIdAndYear(company.getId(), s.getYear())
                .orElseGet(() -> {
                    CompanyFinancial newFin = new CompanyFinancial();
                    newFin.setCompany(company);
                    newFin.setYear(s.getYear());
                    return newFin;
                });

        fin.setRevenue(s.getRevenue());
        fin.setProfitMargin(s.getProfitMargin());
        financialRepo.save(fin);
    }
}