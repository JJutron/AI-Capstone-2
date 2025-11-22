-- V8__skin_profile_skin_type_to_varchar.sql
-- skin_type 컬럼을 ENUM -> VARCHAR(255)로 변경 (Hibernate 기대 타입과 일치)

ALTER TABLE skin_profile
    MODIFY COLUMN skin_type VARCHAR(255) NULL;