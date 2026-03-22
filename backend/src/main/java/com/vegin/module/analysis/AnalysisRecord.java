package com.vegin.module.analysis;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_record")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 유저의 분석인지
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // S3에 저장된 이미지
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // FastAPI에서 반환된 raw 전체 JSON
    @Lob
    @Column(name = "fusion_json", columnDefinition = "TEXT")
    private String fusionJson;

    // 측정 시점
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void init() {
        createdAt = LocalDateTime.now();
    }


}