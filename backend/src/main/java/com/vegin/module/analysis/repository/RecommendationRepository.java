package com.vegin.module.analysis.repository;

import com.vegin.module.analysis.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Recommendation> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    List<Recommendation> findByAnalysisId(Long analysisId);
}