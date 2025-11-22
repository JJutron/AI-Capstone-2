

package com.vegin.module.users.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity @Table(name="skin_profile")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkinProfile {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long userId;
    private String skinType;

    @Column(columnDefinition="json") private String concerns; // JSON string

    @Column(nullable=false) private OffsetDateTime updatedAt;

    @PrePersist @PreUpdate void touch(){ updatedAt = OffsetDateTime.now(); }

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    public SkinProfile withProfileImageUrl(String url) {
        return SkinProfile.builder()
                .id(this.id)
                .userId(this.userId)
                .updatedAt(this.updatedAt)
                .skinType(this.skinType)
                .concerns(this.concerns)
                .profileImageUrl(url)
                .build();
    }

}
