-- V11__create_analysis_record.sql

CREATE TABLE IF NOT EXISTS analysis_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    image_url VARCHAR(500) NULL,
    fusion_json TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analysis_record_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 인덱스 생성 (user_id로 조회 최적화)
SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'analysis_record'
      AND index_name = 'idx_analysis_record_user_created'
);

SET @sql := IF(@idx_exists = 0,
               'CREATE INDEX idx_analysis_record_user_created ON analysis_record (user_id, created_at)',
               'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

