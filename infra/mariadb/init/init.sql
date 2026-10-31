CREATE DATABASE IF NOT EXISTS ums_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ums_db;

-- 메시지 발송 이력 및 상태 관리 테이블
CREATE TABLE IF NOT EXISTS message_history (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               message_id VARCHAR(64) NOT NULL UNIQUE,
    recipient VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;