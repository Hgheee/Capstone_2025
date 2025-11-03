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
 * 사용자 정보를 데이터베이스에 저장하고 관리하는 엔티티 클래스입니다.
 * 'users' 테이블과 매핑됩니다.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "ux_users_email", columnList = "email", unique = true),
        @Index(name = "ux_users_username", columnList = "username", unique = true) // ✅ username 유니크 인덱스
    }
)
@Getter
@NoArgsConstructor
@Slf4j
public class User {

    /** 사용자의 고유 식별자(PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디(username) */
    @Column(name = "username", nullable = false, unique = true, length = 30)
    private String username;

    /** 로그인용 이메일 */
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /** 암호화 저장되는 비밀번호 */
    @Column(nullable = false, length = 255)
    private String password;

    /** 실명 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 연락처(선택) */
    @Column(length = 20)
    private String phone;

    /** 권한 */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    /** 생성일시 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정일시 */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User 객체 생성을 위한 빌더 패턴 생성자입니다.
     *
     * @param username 로그인 아이디 (NOT NULL)
     * @param email    이메일 (NOT NULL)
     * @param password 암호화된 비밀번호 (NOT NULL)
     * @param name     이름 (NOT NULL)
     * @param phone    전화번호 (선택)
     * @param role     권한 (null이면 USER)
     */
    @Builder
    public User(String username, String email, String password, String name, String phone, UserRole role) {
        this.username = username;                 // ✅ 빌더에서 username 세팅
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = (role != null ? role : UserRole.USER);
    }

    /** 비밀번호 변경 */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
        log.info("User password updated for email: {}", this.email);
    }

    /** 이름/전화번호 변경 */
    public void updateUserInfo(String name, String phone) {
        this.name = name;
        this.phone = phone;
        log.info("User info updated for email: {}", this.email);
    }

    /** 관리자 여부 */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}
