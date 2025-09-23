package com.lostfound.capstonebackend.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 블랙리스트에 추가된 토큰({@link BlacklistedToken})에 대한 데이터베이스 작업을 처리하는 리포지토리입니다.
 */
@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    /**
     * 주어진 토큰(JTI)이 블랙리스트에 존재하는지 확인합니다.
     * 전체 엔티티를 조회하는 것보다 성능상 이점이 있습니다.
     *
     * @param token 확인할 토큰의 JTI 문자열
     * @return 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByToken(String token);
}
