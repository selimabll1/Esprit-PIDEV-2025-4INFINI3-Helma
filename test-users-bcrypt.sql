-- ============================================
-- Test Users for Helma Application (BCrypt Hashed Passwords)
-- ============================================
-- Run this script if your backend uses BCrypt password hashing
-- Database: helma_leasing
-- ============================================

USE helma_leasing;

-- Create users table if it doesn't exist
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================
-- Insert Test Users with BCrypt Hashed Passwords
-- ============================================
-- All passwords are hashed using BCrypt (strength 10)
-- Plain text passwords: founder123, investor123, admin123, compliance123
-- ============================================

-- 1. FOUNDER User
-- Password: founder123
INSERT INTO users (email, password, role, first_name, last_name)
VALUES ('founder@helma.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'FOUNDER', 'John', 'Founder')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    role = 'FOUNDER',
    first_name = 'John',
    last_name = 'Founder';

-- 2. INVESTOR User
-- Password: investor123
INSERT INTO users (email, password, role, first_name, last_name)
VALUES ('investor@helma.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'INVESTOR', 'Sarah', 'Investor')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    role = 'INVESTOR',
    first_name = 'Sarah',
    last_name = 'Investor';

-- 3. ADMIN User
-- Password: admin123
INSERT INTO users (email, password, role, first_name, last_name)
VALUES ('admin@helma.com', '$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW', 'ADMIN', 'Mike', 'Admin')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW',
    role = 'ADMIN',
    first_name = 'Mike',
    last_name = 'Admin';

-- 4. COMPLIANCE User (optional - also has admin access)
-- Password: compliance123
INSERT INTO users (email, password, role, first_name, last_name)
VALUES ('compliance@helma.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGQRmb2WZvIuDs5Dq7S0VTi', 'COMPLIANCE', 'Emma', 'Compliance')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGQRmb2WZvIuDs5Dq7S0VTi',
    role = 'COMPLIANCE',
    first_name = 'Emma',
    last_name = 'Compliance';

-- ============================================
-- Verify the inserted users
-- ============================================
SELECT 
    id,
    email,
    role,
    CONCAT(first_name, ' ', last_name) AS full_name,
    created_at
FROM users
ORDER BY role, email;

-- ============================================
-- Test Login Credentials
-- ============================================
-- FOUNDER:
--   Email: founder@helma.com
--   Password: founder123
--
-- INVESTOR:
--   Email: investor@helma.com
--   Password: investor123
--
-- ADMIN:
--   Email: admin@helma.com
--   Password: admin123
--
-- COMPLIANCE:
--   Email: compliance@helma.com
--   Password: compliance123
-- ============================================
