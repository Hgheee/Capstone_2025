
-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 인덱스를 테이블 생성과 함께 정의 (안전)
    INDEX idx_users_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 분실물 테이블
CREATE TABLE IF NOT EXISTS lost_item (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         title VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    location VARCHAR(200),
    found_date DATE,
    status ENUM('FOUND', 'CLAIMED', 'EXPIRED') DEFAULT 'FOUND',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 성능 최적화 인덱스
    INDEX idx_lost_item_status (status),
    INDEX idx_lost_item_found_date (found_date),
    INDEX idx_lost_item_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- JWT 블랙리스트 토큰 테이블
CREATE TABLE IF NOT EXISTS blacklisted_token (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 만료된 토큰 정리용 인덱스
    INDEX idx_expires_at (expires_at),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
