package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "shared_api")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
@EntityListeners(AuditingEntityListener.class)
public class SharedApi {

    @Id
    @Column(name = "shared_api_id", length = 36, nullable = false)
    private String sharedApiId;

    @Column(name = "original_api_id", length = 36, nullable = false)
    private String originalApiId;

    @Column(name = "creator_id", length = 36, nullable = false)
    private String creatorId;

    @Column(name = "api_name", length = 255, nullable = false)
    private String apiName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_count", nullable = false)
    private Integer dataCount;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void deactivate() {
        this.isActive = false;
    }

    public void reactivate() {
        this.isActive = true;
    }

    public void updateInfo(String apiName, String description) {
        if (apiName != null && !apiName.trim().isEmpty()) {
            this.apiName = apiName.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
    }

    public boolean isValid() {
        return sharedApiId != null && !sharedApiId.trim().isEmpty() &&
               originalApiId != null && !originalApiId.trim().isEmpty() &&
               creatorId != null && !creatorId.trim().isEmpty() &&
               apiName != null && !apiName.trim().isEmpty() &&
               dataCount != null && dataCount >= 0 &&
               createdAt != null;
    }

    public boolean isCreatedBy(String userId) {
        return creatorId != null && creatorId.equals(userId);
    }

    public boolean isActive() {
        return isActive != null && isActive;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedApi sharedApi = (SharedApi) o;
        return Objects.equals(sharedApiId, sharedApi.sharedApiId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sharedApiId);
    }
}