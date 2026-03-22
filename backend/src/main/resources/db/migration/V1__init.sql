-- users
CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL,
                       nickname VARCHAR(30),
                       gender ENUM('M','F','OTHER'),
                       birth_date DATE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- skin_profile
CREATE TABLE skin_profile (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              user_id BIGINT NOT NULL,
                              skin_type ENUM('건성','지성','복합성','수부지'),
                              concerns TEXT,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              CONSTRAINT fk_skin_profile_user
                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- skin_analysis
CREATE TABLE skin_analysis (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               s3_key VARCHAR(255) NOT NULL,
                               survey JSON,
                               status ENUM('PENDING','DONE') DEFAULT 'PENDING',
                               result JSON,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_skin_analysis_user
                                   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product (Mongo 연동 메타)
CREATE TABLE product (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         mongo_id VARCHAR(24) NOT NULL,
                         name VARCHAR(100) NOT NULL,
                         brand VARCHAR(80),
                         category ENUM('TONER','SERUM','CREAM','ESSENCE','LOTION','SUN','ETC'),
                         vegan_flag BOOLEAN DEFAULT TRUE,
                         image_url VARCHAR(200),
                         product_url VARCHAR(500),
                         ingredients TEXT,
                         price INT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

