-- V6__alter_skin_analysis_status_to_varchar.sql
-- status 컬럼이 ENUM일 때만 VARCHAR(30)로 변경 (idempotent)

SET @is_enum := (
    SELECT CASE WHEN DATA_TYPE = 'enum' THEN 1 ELSE 0 END
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'skin_analysis'
      AND column_name  = 'status'
    LIMIT 1
);

-- 필요하면 기본값을 원하는 값으로 바꿔줘 (예: 'PENDING')
SET @alter_sql := IF(
            @is_enum = 1,
            'ALTER TABLE skin_analysis MODIFY status VARCHAR(30) NOT NULL DEFAULT ''PENDING''',
            'SELECT 1'
    );

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;