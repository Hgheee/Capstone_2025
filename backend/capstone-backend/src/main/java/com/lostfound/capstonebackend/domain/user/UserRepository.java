package com.lostfound.capstonebackend.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 데이터 접근 레포지토리
 * JPA를 통한 User 엔티티의 CRUD 작업을 담당합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     * 로그인 시 사용자 인증에 사용됩니다.
     *
     * @param email 조회할 사용자 이메일
     * @return 해당 이메일의 사용자 (존재하지 않으면 Optional.empty())
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 중복 검사
     * 회원가입 시 이메일 중복을 확인합니다.
     *
     * @param email 검사할 이메일 주소
     * @return true: 이메일 존재, false: 이메일 미존재
     */
    boolean existsByEmail(String email);

    /**
     * ID와 이메일로 사용자 조회 (보안 검증용)
     * 추가적인 보안 검증이 필요한 경우 사용됩니다.
     *
     * @param id    사용자 ID
     * @param email 사용자 이메일
     * @return 해당 조건의 사용자 (존재하지 않으면 Optional.empty())
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.email = :email")
    Optional<User> findByIdAndEmail(@Param("id") Long id, @Param("email") String email);

    /**
     * 이름으로 사용자 검색 (부분 일치)
     * 관리자 기능에서 사용자 검색 시 활용됩니다.
     *
     * @param name 검색할 이름 (부분 일치)
     * @return 해당 이름을 포함하는 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
    java.util.List<User> findByNameContaining(@Param("name") String name);
}