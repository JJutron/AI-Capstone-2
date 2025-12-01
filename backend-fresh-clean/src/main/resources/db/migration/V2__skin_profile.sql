CREATE TABLE IF NOT EXISTS skin_profile (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            user_id BIGINT NOT NULL,
                                            skin_type VARCHAR(30),
                                            concerns JSON,
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            CONSTRAINT fk_skin_profile_user FOREIGN KEY (user_id) REFERENCES users(id)
);