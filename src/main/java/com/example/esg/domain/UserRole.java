package com.example.esg.domain;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("ROLE_ADMIN"),   // 시스템 관리자
    CORP("ROLE_CORP"),     // 기업 사용자
    PUBLIC("ROLE_PUBLIC"); // 일반 사용자

    UserRole(String value) {
        this.value = value;
    }

    private final String value;
}