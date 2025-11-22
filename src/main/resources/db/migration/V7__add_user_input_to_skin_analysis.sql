-- 옵션 A: 길이 제한 문자열
ALTER TABLE skin_analysis
    ADD COLUMN user_input VARCHAR(1000) NULL;