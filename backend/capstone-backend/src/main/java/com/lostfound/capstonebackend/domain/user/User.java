package com.lostfound.capstonebackend.domain.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티 클래스
 * 분실물 관리 시스템의 사용자 정보를 관리합니다.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "ux_users_email", columnList = "email", unique = true),
        @Index(name = "ux_users_username", columnList = "username", unique = true)
})
@Getter
@NoArgsConstructor
@Slf4j
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String username;  // ✅ 추가됨

    @Column(length = 20)
    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String password, String name, String username, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.username = username;
        this.phone = phone;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
        log.info("User password updated for email: {}", this.email);
    }

    public void updateUserInfo(String name, String username, String phone) {
        this.name = name;
        this.username = username;
        this.phone = phone;
        log.info("User info updated for email: {}", this.email);
    }
}
