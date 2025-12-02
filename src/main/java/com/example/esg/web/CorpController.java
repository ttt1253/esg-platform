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
    private final EtlHistoryRepo historyRepo;

    // 1. 내 기업 데이터 목록 조회
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

    // 2. 데이터 추가 및 수정 처리 (로그 상세화 적용)
    @PostMapping("/data/update")
    public String corpDataUpdate(@RequestParam(required = false) Long id,
                                 @ModelAttribute StagingEsg formData,
                                 Principal principal) {

        SiteUser user = userRepo.findByUsername(principal.getName()).orElseThrow();
        if (user.getCompany() == null) return "redirect:/";
        String myCompanyName = user.getCompany().getName();

        StagingEsg data;
        String logDetails = ""; // ✅ 로그에 남길 상세 메시지

        // 1. 수정 모드
        if (id != null) {
            data = stagingRepo.findById(id).orElse(new StagingEsg());
            if (!data.getCompanyName().equals(myCompanyName)) {
                return "redirect:/corp/data?error=unauthorized";
            }

            // ✅ [핵심] 덮어쓰기 전에 기존 값 vs 새 값 비교해서 로그 만들기
            logDetails = generateChangeLog(data, formData);
        }
        // 2. 추가 모드
        else {
            data = new StagingEsg();
            logDetails = "신규 데이터 추가: " + myCompanyName + " (" + formData.getYear() + ")";
        }

        // 값 채우기 (회사명 고정)
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

        // ID 부여 로직
        Long existingId = stagingRepo.findCompanyIdByName(myCompanyName).orElse(null);
        if (existingId != null) data.setCompanyId(existingId);
        else {
            Long maxId = stagingRepo.findMaxCompanyId().orElse(0L);
            data.setCompanyId(maxId + 1);
        }

        stagingRepo.save(data);
        syncToRealTables(data);

        // ✅ [수정] 상세 변경 내용을 로그에 저장
        String jobName = (id != null) ? "기업 데이터 수정" : "기업 데이터 추가";
        logHistory(jobName, "SUCCESS", logDetails, principal.getName());

        return "redirect:/corp/data";
    }

    // 3. 데이터 삭제 처리
    @GetMapping("/data/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String corpDataDelete(@PathVariable Long id, Principal principal) {

        SiteUser user = userRepo.findByUsername(principal.getName()).orElseThrow();
        String myCompanyName = user.getCompany().getName();

        StagingEsg target = stagingRepo.findById(id).orElse(null);

        if (target != null && target.getCompanyName().equals(myCompanyName)) {
            String logDetail = myCompanyName + " (" + target.getYear() + ")";

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
            logHistory("기업 데이터 삭제", "SUCCESS", logDetail + " 삭제됨", principal.getName());
        }
        return "redirect:/corp/data";
    }

    private void logHistory(String job, String status, String details, String worker) {
        EtlHistory h = new EtlHistory();
        h.setJobName(job);
        h.setStatus(status);
        h.setDetails(details);
        h.setWorker(worker);
        historyRepo.save(h);
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

    // ✅ [추가] 변경 사항 감지 헬퍼 메서드 (AdminController와 동일)
    private String generateChangeLog(StagingEsg oldData, StagingEsg newData) {
        StringBuilder sb = new StringBuilder();

        // (기업회원은 회사명 변경 불가하므로 비교 생략 가능하지만 안전을 위해 포함)
        checkObjectChange(sb, "연도", oldData.getYear(), newData.getYear());
        checkStringChange(sb, "산업군", oldData.getIndustry(), newData.getIndustry());
        checkStringChange(sb, "지역", oldData.getRegion(), newData.getRegion());

        checkObjectChange(sb, "ESG종합", oldData.getEsgOverall(), newData.getEsgOverall());
        checkObjectChange(sb, "Env", oldData.getEsgEnvironmental(), newData.getEsgEnvironmental());
        checkObjectChange(sb, "Soc", oldData.getEsgSocial(), newData.getEsgSocial());
        checkObjectChange(sb, "Gov", oldData.getEsgGovernance(), newData.getEsgGovernance());

        checkObjectChange(sb, "매출", oldData.getRevenue(), newData.getRevenue());
        checkObjectChange(sb, "이익률", oldData.getProfitMargin(), newData.getProfitMargin());

        if (sb.length() == 0) return "변경 사항 없음";
        return sb.substring(0, sb.length() - 2);
    }

    private void checkStringChange(StringBuilder sb, String label, String oldVal, String newVal) {
        String o = (oldVal == null) ? "" : oldVal;
        String n = (newVal == null) ? "" : newVal;
        if (!o.equals(n)) sb.append(String.format("%s: %s -> %s, ", label, o, n));
    }

    private void checkObjectChange(StringBuilder sb, String label, Object oldVal, Object newVal) {
        if (oldVal == null && newVal == null) return;
        if ((oldVal == null && newVal != null) || (oldVal != null && !oldVal.equals(newVal))) {
            sb.append(String.format("%s: %s -> %s, ", label, oldVal, newVal));
        }
    }
}