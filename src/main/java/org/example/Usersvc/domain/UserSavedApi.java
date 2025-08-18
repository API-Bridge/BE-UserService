package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_saved_api")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
@EntityListeners(AuditingEntityListener.class)
public class UserSavedApi {

    @Id
    @Column(name = "user_api_id", length = 36, nullable = false)
    private String userApiId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "shared_api_id", length = 36, nullable = false)
    private String sharedApiId;

    @Column(name = "api_name", length = 255, nullable = false)
    private String apiName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_count", nullable = false)
    private Integer dataCount;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateApiInfo(String apiName, String description) {
        if (apiName != null && !apiName.trim().isEmpty()) {
            this.apiName = apiName.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public boolean isValid() {
        return userApiId != null && !userApiId.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               sharedApiId != null && !sharedApiId.trim().isEmpty() &&
               apiName != null && !apiName.trim().isEmpty() &&
               dataCount != null && dataCount >= 0 &&
               createdAt != null;
    }

    public boolean isOwnedBy(String userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public boolean isBasedOnSharedApi(String sharedApiId) {
        return this.sharedApiId != null && this.sharedApiId.equals(sharedApiId);
    }

    public boolean isDeleted() {
        return isDeleted != null && isDeleted;
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
        UserSavedApi that = (UserSavedApi) o;
        return Objects.equals(userApiId, that.userApiId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userApiId);
    }
}