package com.vegin.module.analysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name="recommendation")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;
    private Long analysisId;

    @Column(columnDefinition="json", nullable=false)
    private String items; // Top3, xai_text
    @Column(nullable=false)
    private OffsetDateTime createdAt;

    @PrePersist
    void init(){ createdAt = OffsetDateTime.now(); }
}
