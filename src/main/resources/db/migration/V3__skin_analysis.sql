CREATE TABLE IF NOT EXISTS skin_analysis (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             user_id BIGINT NOT NULL,
                                             s3_key VARCHAR(500) NOT NULL,
                                             user_input JSON,
                                             status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                             result JSON,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             CONSTRAINT fk_skin_analysis_user FOREIGN KEY (user_id) REFERENCES users(id)
);