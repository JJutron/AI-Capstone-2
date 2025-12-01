package com.vegin.module.analysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name="skin_analysis")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkinAnalysis {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Column(name = "s3_key", nullable=true, length=500)
    private String s3Key;

    @Column(name = "user_input", length = 1000, columnDefinition="json")
    private String userInput; // 설문 JSON

    @Column(name = "status", nullable=false, length=30)
    private String status;  // PENDING/DONE/FAILED

    @Column(columnDefinition="json")
    private String result;    // 분석 결과 JSON

    @Column(name = "created_at", nullable=false)
    private OffsetDateTime createdAt;

    @PrePersist void init(){ createdAt = OffsetDateTime.now(); }
}
