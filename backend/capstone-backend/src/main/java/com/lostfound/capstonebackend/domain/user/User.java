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
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "ux_users_email", columnList = "email", unique = true)
})
@Getter
@NoArgsConstructor
@Slf4j
public class User {

    /**
     * 사용자 고유 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 이메일 주소 (Unique, 로그인 ID로 사용)
     */
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * 사용자 비밀번호 (BCrypt 암호화)
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 사용자 이름
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 사용자 전화번호 (선택사항)
     */
    @Column(length = 20)
    private String phone;

    /**
     * 사용자 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 사용자 정보 수정 일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User 객체 생성자 (Builder 패턴)
     *
     * @param email    사용자 이메일
     * @param password 암호화된 비밀번호
     * @param name     사용자 이름
     * @param phone    전화번호 (선택사항)
     */
    @Builder
    public User(String email, String password, String name, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
    }

    /**
     * 비밀번호 업데이트 메소드
     *
     * @param newPassword 새로운 암호화된 비밀번호
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
        log.info("User password updated for email: {}", this.email);
    }

    /**
     * 사용자 정보 업데이트 메소드
     *
     * @param name  새로운 이름
     * @param phone 새로운 전화번호
     */
    public void updateUserInfo(String name, String phone) {
        this.name = name;
        this.phone = phone;
        log.info("User info updated for email: {}", this.email);
    }
}
