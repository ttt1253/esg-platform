package com.example.esg.service;

import com.example.esg.domain.Company;
import com.example.esg.domain.SiteUser;
import com.example.esg.domain.UserRole;
import com.example.esg.repo.UserRepo;
import com.example.esg.repo.CompanyRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepo companyRepo;

    public SiteUser create(String username, String password, String roleType, String companyName) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        if ("CORP".equals(roleType)) {
            user.setRole(UserRole.CORP);

            if (companyName != null && !companyName.isBlank()) {
                Company company = companyRepo.findAll().stream()
                        .filter(c -> c.getName().equalsIgnoreCase(companyName))
                        .findFirst()
                        .orElseGet(() -> {
                            Company newComp = new Company();
                            newComp.setName(companyName);
                            return companyRepo.save(newComp);
                        });
                user.setCompany(company);
            }
        } else {
            user.setRole(UserRole.PUBLIC);
        }

        return userRepo.save(user);
    }
}