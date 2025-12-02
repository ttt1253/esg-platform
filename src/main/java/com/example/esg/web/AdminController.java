package com.example.esg.web;

import com.example.esg.domain.*;
import com.example.esg.repo.*;
import com.example.esg.domain.SiteUser;
import com.example.esg.domain.StagingEsg;
import com.example.esg.domain.UserRole;
import com.example.esg.repo.StagingEsgRepo;
import com.example.esg.repo.UserRepo;
import com.example.esg.domain.EtlHistory;
import com.example.esg.repo.EtlHistoryRepo;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;


import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StagingEsgRepo stagingRepo;
    private final UserRepo userRepo;

    private final CompanyRepo companyRepo;
    private final EsgScoreRepo esgScoreRepo;
    private final CompanyFinancialRepo financialRepo;
    private final EtlHistoryRepo historyRepo;

    @GetMapping("/data")
    public String dataList(Model model, @RequestParam(required = false) String keyword) {
        List<StagingEsg> list;

        // ✅ 정렬 객체 생성: ID 기준 오름차순 (1, 2, 3...)
        Sort sort = Sort.by(Sort.Direction.ASC, "id");

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색할 때도 정렬이 필요하면 Repository 수정이 필요하지만, 일단 전체 조회만이라도 정렬 적용
            list = stagingRepo.findByCompanyNameContainingIgnoreCase(keyword);
        } else {
            // ✅ findAll(sort) 를 사용하여 순서 고정
            list = stagingRepo.findAll(sort);
        }

        model.addAttribute("list", list);
        model.addAttribute("keyword", keyword);
        return "admin/data_list";
    }

    @GetMapping("/history")
    public String historyList(Model model) {
        List<EtlHistory> history = historyRepo.findAllByOrderByCreatedAtDesc();
        model.addAttribute("history", history);
        return "admin/history"; // templates/admin/history.html
    }

    @GetMapping("/history/delete/{id}")
    public String deleteHistory(@PathVariable Long id) {
        historyRepo.deleteById(id);
        return "redirect:/admin/history";
    }

    @PostMapping("/history/clear")
    public String clearHistory() {
        historyRepo.deleteAll();
        return "redirect:/admin/history";
    }

    // 데이터 수정 처리
    @PostMapping("/data/update")
    public String dataUpdate(@RequestParam(required = false) Long id,
                             @ModelAttribute StagingEsg formData,
                             Principal principal) {

        StagingEsg data;
        String logDetails = "";

        if (id != null) {
            data = stagingRepo.findById(id).orElse(new StagingEsg());

            logDetails = generateChangeLog(data, formData);
        }
        else {
            data = new StagingEsg();
            logDetails = "신규 데이터 추가: " + formData.getCompanyName();
        }

        data.setCompanyName(formData.getCompanyName());
        data.setYear(formData.getYear());
        data.setIndustry(formData.getIndustry());
        data.setRegion(formData.getRegion());

        data.setEsgOverall(formData.getEsgOverall());
        data.setEsgEnvironmental(formData.getEsgEnvironmental());
        data.setEsgSocial(formData.getEsgSocial());
        data.setEsgGovernance(formData.getEsgGovernance());

        data.setRevenue(formData.getRevenue());
        data.setProfitMargin(formData.getProfitMargin());

        Long existingId = stagingRepo.findCompanyIdByName(data.getCompanyName()).orElse(null);
        if (existingId != null) {
            data.setCompanyId(existingId);
        } else {
            Long maxId = stagingRepo.findMaxCompanyId().orElse(0L);
            data.setCompanyId(maxId + 1);
        }

        // 저장 및 동기화
        stagingRepo.save(data);
        syncToRealTables(data);

        String jobName = (id != null) ? "데이터 수정" : "데이터 추가";
        logHistory(jobName, "SUCCESS", logDetails, principal.getName());

        return "redirect:/admin/data";
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

    // 데이터 삭제 처리
    @GetMapping("/data/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String dataDelete(@PathVariable Long id, Principal principal) {

        StagingEsg target = stagingRepo.findById(id).orElse(null);

        if (target != null) {
            String targetInfo = target.getCompanyName() + " (" + target.getYear() + ")";
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
            logHistory("데이터 삭제", "SUCCESS", targetInfo + " 삭제됨", principal.getName());
        }

        return "redirect:/admin/data";
    }

    private void logHistory(String job, String status, String details, String worker) {
        EtlHistory h = new EtlHistory();
        h.setJobName(job);
        h.setStatus(status);
        h.setDetails(details);
        h.setWorker(worker);
        historyRepo.save(h);
    }

    @GetMapping("/users")
    public String userList(Model model) {
        List<SiteUser> users = userRepo.findAll();
        model.addAttribute("users", users);
        return "admin/user_list"; // templates/admin/user_list.html
    }

    @PostMapping("/users/role")
    public String updateUserRole(@RequestParam Long userId, @RequestParam String role) {
        SiteUser user = userRepo.findById(userId).orElseThrow();

        if (user.getRole() == UserRole.ADMIN) {
            return "redirect:/admin/users?error=admin";
        }

        if ("CORP".equals(role)) {
            user.setRole(UserRole.CORP);
        } else {
            user.setRole(UserRole.PUBLIC);
        }
        userRepo.save(user);

        return "redirect:/admin/users";
    }

    private String generateChangeLog(StagingEsg oldData, StagingEsg newData) {
        StringBuilder sb = new StringBuilder();

        if (!oldData.getCompanyName().equals(newData.getCompanyName())) {
            sb.append(String.format("회사명: %s -> %s, ", oldData.getCompanyName(), newData.getCompanyName()));
        }
        if (!oldData.getYear().equals(newData.getYear())) {
            sb.append(String.format("연도: %d -> %d, ", oldData.getYear(), newData.getYear()));
        }
        checkChange(sb, "ESG종합", oldData.getEsgOverall(), newData.getEsgOverall());
        checkChange(sb, "Env", oldData.getEsgEnvironmental(), newData.getEsgEnvironmental());
        checkChange(sb, "Soc", oldData.getEsgSocial(), newData.getEsgSocial());
        checkChange(sb, "Gov", oldData.getEsgGovernance(), newData.getEsgGovernance());

        checkChange(sb, "매출", oldData.getRevenue(), newData.getRevenue());
        checkChange(sb, "이익률", oldData.getProfitMargin(), newData.getProfitMargin());

        if (sb.length() == 0) return "변경 사항 없음";

        return sb.substring(0, sb.length() - 2);
    }

    private void checkChange(StringBuilder sb, String label, Object oldVal, Object newVal) {
        if (oldVal == null && newVal == null) return;

        if ((oldVal == null && newVal != null) ||
                (oldVal != null && !oldVal.equals(newVal))) {
            sb.append(String.format("%s: %s -> %s, ", label, oldVal, newVal));
        }
    }
}