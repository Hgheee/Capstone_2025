-- =====================================================
-- 분실물 통합 조회 서비스 데이터베이스 스키마
-- 데이터베이스: capstonedb
-- =====================================================

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 분실물 테이블 (완전한 스키마)
CREATE TABLE IF NOT EXISTS lost_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 기본 정보
    title VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    
    -- 위치 정보
    location VARCHAR(200),
    storage_location VARCHAR(200),
    
    -- 날짜 정보
    found_date DATE,
    
    -- 상태 정보
    status VARCHAR(20) NOT NULL DEFAULT 'FOUND',
    
    -- 외부 데이터 정보
    external_id VARCHAR(100),
    datasource VARCHAR(20) NOT NULL DEFAULT 'USER',
    view_count INT,
    received_date TIMESTAMP,
    
    -- 추가 속성
    color VARCHAR(50),
    image_path VARCHAR(255),
    
    -- 소유자 정보
    user_id BIGINT,
    
    -- 타임스탬프
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 외래키
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- 인덱스
    INDEX idx_lost_item_status (status),
    INDEX idx_lost_item_found_date (found_date),
    INDEX idx_lost_item_created_at (created_at),
    INDEX idx_lost_item_category (category),
    INDEX idx_lost_item_external_id (external_id),
    INDEX idx_lost_item_datasource (datasource),
    INDEX idx_lost_item_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 블랙리스트 토큰 테이블
CREATE TABLE IF NOT EXISTS blacklisted_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_blacklisted_token_token (token),
    INDEX idx_blacklisted_token_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 유니크 인덱스
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users(email);
CREATE UNIQUE INDEX IF NOT EXISTS ux_blacklisted_token_token ON blacklisted_token(token);

-- 샘플 데이터 (테스트용)
INSERT IGNORE INTO users (email, password, name, phone, role) VALUES
('test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트 사용자', '010-1234-5678', 'USER'),
('admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '관리자', '010-9999-9999', 'ADMIN');

-- 샘플 분실물 데이터
INSERT IGNORE INTO lost_item (title, description, category, location, found_date, status, color, datasource, user_id) VALUES
('검은색 지갑', '신한은행 카드가 들어있는 가죽 지갑', '지갑', '강남역 2번 출구', '2025-01-15', 'FOUND', '검은색', 'USER', 1),
('빨간색 우산', '접이식 자동 우산', '우산', '서초구 서초대로', '2025-01-14', 'FOUND', '빨간색', 'USER', 1),
('아이폰 15', '흰색 실리콘 케이스 장착', '핸드폰', '강남구 테헤란로', '2025-01-13', 'FOUND', '흰색', 'USER', 1),
('노트북 가방', '검은색 백팩 형태의 노트북 가방', '가방', '송파구 올림픽로', '2025-01-12', 'FOUND', '검은색', 'USER', 1),
('에어팟', '에어팟 프로 2세대 케이스', '귀중품', '강동구 천호역', '2025-01-11', 'FOUND', '흰색', 'USER', 1);
