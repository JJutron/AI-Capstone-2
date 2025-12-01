-- V4__recommendation.sql

CREATE TABLE IF NOT EXISTS recommendation (
                                              id           BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              user_id      BIGINT NOT NULL,
                                              analysis_id  BIGINT NULL,
                                              items        JSON NOT NULL,
                                              created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT fk_rec_user     FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
                                              CONSTRAINT fk_rec_analysis FOREIGN KEY (analysis_id)  REFERENCES skin_analysis(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- (옵션) FK 컬럼 단일 인덱스는 InnoDB가 자동 생성하지만,
-- 조회 최적화를 위한 복합 인덱스는 직접 생성.
-- MySQL 구버전 호환: IF NOT EXISTS 대신 information_schema로 존재 여부 체크
SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name   = 'recommendation'
      AND index_name   = 'idx_rec_user_created'
);

SET @sql := IF(@idx_exists = 0,
               'CREATE INDEX idx_rec_user_created ON recommendation (user_id, created_at)',
               'SELECT 1'
    );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;