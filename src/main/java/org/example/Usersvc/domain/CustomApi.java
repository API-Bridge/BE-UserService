/* CustomAPI 기능 분리 - Custom API Service로 이관
package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "custom_api")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
@EntityListeners(AuditingEntityListener.class)
public class CustomApi {

    @Id
    @Column(name = "custom_api_id", length = 36, nullable = false)
    private String customApiId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // data_count와 is_deleted 필드는 새 스키마에서 제거됨

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateApiInfo(String name, String description) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
    }

    // updateDataCount와 markAsDeleted 메서드는 새 스키마에서 제거됨

    public boolean isValid() {
        return customApiId != null && !customApiId.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               name != null && !name.trim().isEmpty() &&
               createdAt != null;
    }

    // canShareForPlan과 isDeleted 메서드는 새 스키마에서 제거됨

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
        return Objects.equals(customApiId, customApi.customApiId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customApiId);
    }
}*/
