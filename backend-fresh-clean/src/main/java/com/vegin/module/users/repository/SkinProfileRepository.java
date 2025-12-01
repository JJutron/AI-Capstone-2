package com.vegin.module.users.repository;

import com.vegin.module.users.Entity.SkinProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkinProfileRepository extends JpaRepository<SkinProfile, Long> {
    Optional<SkinProfile> findByUserId(Long userId);

}

