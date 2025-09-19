-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 분실물 테이블 (Week 1 placeholder)
CREATE TABLE IF NOT EXISTS lost_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 블랙리스트 토큰 테이블 (없으면 생성)
CREATE TABLE IF NOT EXISTS blacklisted_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(200) NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

-- 유니크 인덱스 (존재하면 무시)
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users(email);
CREATE UNIQUE INDEX IF NOT EXISTS ux_blacklisted_token_token ON blacklisted_token(token);

