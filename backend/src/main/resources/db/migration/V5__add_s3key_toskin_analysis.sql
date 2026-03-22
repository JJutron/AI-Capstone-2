-- V5__add_s3key_to_skin_analysis.sql

-- 1) 컬럼 존재 여부 확인 후 없으면 추가 (우선 NULL 허용으로 안전하게 추가)
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'skin_analysis'
      AND column_name  = 's3key'
);

SET @add_col_sql := IF(
            @col_exists = 0,
            'ALTER TABLE skin_analysis ADD COLUMN s3key VARCHAR(512) NULL',
            'SELECT 1'
    );

PREPARE stmt FROM @add_col_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) (선택) s3key 조회 최적화를 위한 인덱스 생성 (중복 생성 방지)
SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name   = 'skin_analysis'
      AND index_name   = 'idx_skin_analysis_s3key'
);

SET @add_idx_sql := IF(
            @idx_exists = 0,
            'CREATE INDEX idx_skin_analysis_s3key ON skin_analysis (s3key)',
            'SELECT 1'
    );

PREPARE stmt2 FROM @add_idx_sql;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;