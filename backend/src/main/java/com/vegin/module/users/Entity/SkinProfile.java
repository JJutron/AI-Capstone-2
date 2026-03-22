

package com.vegin.module.users.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "skin_profile")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkinProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "skin_type", length = 30)
    private String skinType;

    @Column(columnDefinition = "json")
    private String concerns; // JSON string

    @Column(name = "mbti", length = 10)
    private String mbti;

    @Column(name = "tone", length = 30)
    private String tone;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public SkinProfile withProfileImageUrl(String url) {
        return SkinProfile.builder()
                .id(this.id)
                .userId(this.userId)
                .updatedAt(this.updatedAt)
                .skinType(this.skinType)
                .concerns(this.concerns)
                .mbti(this.mbti)
                .tone(this.tone)
                .profileImageUrl(url)
                .build();
    }
}
