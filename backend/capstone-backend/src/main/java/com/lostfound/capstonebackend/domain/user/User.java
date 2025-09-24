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
@Table(name = "users", indexes = {
        @Index(name = "ux_users_email", columnList = "email", unique = true)
})
@Getter
@NoArgsConstructor
@Slf4j
public class User {

    /**
     * 사용자의 고유 식별자(PK)입니다. 데이터베이스에서 자동으로 생성됩니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자의 이메일 주소이며, 로그인 시 ID로 사용됩니다. 중복될 수 없습니다.
     */
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * 암호화되어 저장되는 사용자의 비밀번호입니다.
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 사용자의 실명입니다.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 사용자의 연락처(전화번호)입니다. 선택적으로 입력할 수 있습니다.
     */
    @Column(length = 20)
    private String phone;

    /**
     * 사용자의 권한 등급을 나타냅니다. (예: USER, ADMIN)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    /**
     * 레코드가 생성된 일시입니다. 자동으로 현재 시간이 기록됩니다.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 레코드가 마지막으로 수정된 일시입니다. 자동으로 현재 시간이 기록됩니다.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User 객체 생성을 위한 빌더 패턴 생성자입니다.
     *
     * @param email    사용자 이메일
     * @param password 암호화된 비밀번호
     * @param name     사용자 이름
     * @param phone    전화번호 (선택)
     * @param role     사용자 권한 (기본값: USER)
     */
    @Builder
    public User(String email, String password, String name, String phone, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role != null ? role : UserRole.USER;
    }

    /**
     * 사용자의 비밀번호를 변경합니다.
     * @param newPassword 암호화된 새로운 비밀번호
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
        log.info("User password updated for email: {}", this.email);
    }

    /**
     * 사용자의 이름과 전화번호를 수정합니다.
     * @param name 새로운 이름
     * @param phone 새로운 전화번호
     */
    public void updateUserInfo(String name, String phone) {
        this.name = name;
        this.phone = phone;
        log.info("User info updated for email: {}", this.email);
    }

    /**
     * 현재 사용자가 관리자(ADMIN) 권한을 가지고 있는지 확인합니다.
     * @return 관리자일 경우 true, 아닐 경우 false
     */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}
