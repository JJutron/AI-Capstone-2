package com.vegin.module.analysis.repository;

import com.vegin.module.analysis.domain.SkinAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkinAnalysisRepository extends JpaRepository<SkinAnalysis, Long> {
    List<SkinAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}