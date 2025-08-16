package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "custom_apis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
@EntityListeners(AuditingEntityListener.class)
public class CustomApi {

    @Id
    @Column(name = "api_id", length = 36, nullable = false)
    private String apiId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "api_name", length = 255, nullable = false)
    private String apiName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_count", nullable = false)
    private Integer dataCount;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

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

    public void updateDataCount(int dataCount) {
        if (dataCount < 0) {
            throw new IllegalArgumentException("데이터 수는 0 이상이어야 합니다.");
        }
        this.dataCount = dataCount;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    public boolean isValid() {
        return apiId != null && !apiId.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               apiName != null && !apiName.trim().isEmpty() &&
               dataCount != null && dataCount >= 0 &&
               createdAt != null;
    }

    public boolean canShareForPlan(String planType) {
        if ("FREE".equals(planType)) {
            return dataCount <= 3;
        } else if ("PRO".equals(planType)) {
            return dataCount <= 20;
        } else if ("ENTERPRISE".equals(planType)) {
            return true;
        }
        return false;
    }

    public boolean isDeleted() {
        return deleted != null && deleted;
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
        CustomApi customApi = (CustomApi) o;
        return Objects.equals(apiId, customApi.apiId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiId);
    }
}