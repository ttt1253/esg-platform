package com.example.esg.service;

import com.example.esg.domain.SiteUser;
import com.example.esg.domain.UserRole;
import com.example.esg.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserSecurityService implements UserDetailsService {

    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // 변수명도 username으로 통일

        // ✅ findByEmail -> findByUsername 으로 변경
        Optional<SiteUser> _siteUser = this.userRepo.findByUsername(username);

        if (_siteUser.isEmpty()) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        SiteUser siteUser = _siteUser.get();

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (UserRole.ADMIN.equals(siteUser.getRole())) {
            authorities.add(new SimpleGrantedAuthority(UserRole.ADMIN.getValue()));
        } else if (UserRole.CORP.equals(siteUser.getRole())) {
            authorities.add(new SimpleGrantedAuthority(UserRole.CORP.getValue()));
        } else {
            authorities.add(new SimpleGrantedAuthority(UserRole.PUBLIC.getValue()));
        }

        return new User(siteUser.getUsername(), siteUser.getPassword(), authorities);
    }
}