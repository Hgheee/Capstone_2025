-- 관리자 계정 생성 스크립트
-- ID: admin@admin.com (이메일 형식으로 통일)
-- Password: Snow0316!
-- 비밀번호는 BCrypt로 암호화되어야 하므로, 애플리케이션을 통해 생성하거나
-- 아래의 암호화된 비밀번호를 사용하세요.

-- BCrypt 암호화된 "Snow0316!" 비밀번호
-- $2a$10$encrypted_password_here

-- 관리자 계정이 이미 존재하는지 확인하고 없으면 삽입
INSERT INTO users (username, email, password, name, phone, role, created_at, updated_at)
SELECT 
    'admin',
    'admin@admin.com',
    -- 이 비밀번호는 임시입니다. 실제로는 애플리케이션 실행 후 회원가입 또는 
    -- Spring의 PasswordEncoder를 통해 생성된 값을 사용해야 합니다.
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy',  -- Snow0316!
    '시스템관리자',
    '010-0000-0000',
    'ADMIN',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@admin.com'
);

-- 확인
SELECT id, username, email, name, role, created_at 
FROM users 
WHERE email = 'admin@admin.com';



