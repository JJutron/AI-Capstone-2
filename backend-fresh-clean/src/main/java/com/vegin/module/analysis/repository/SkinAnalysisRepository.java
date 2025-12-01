package com.vegin.module.analysis.repository;

import com.vegin.module.analysis.domain.SkinAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SkinAnalysisRepository extends JpaRepository<SkinAnalysis, Long> {
    List<SkinAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SkinAnalysis> findByIdAndUserId(Long id, Long userId);
}