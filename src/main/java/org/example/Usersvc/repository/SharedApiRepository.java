/* 공유 기능 비활성화
package org.example.Usersvc.repository;

import org.example.Usersvc.domain.SharedApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedApiRepository extends JpaRepository<SharedApi, String> {

    List<SharedApi> findByIsActiveTrueOrderByCreatedAtDesc();

    Page<SharedApi> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    List<SharedApi> findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(String creatorId);

    Optional<SharedApi> findByOriginalApiIdAndIsActiveTrue(String originalApiId);

    List<SharedApi> findByApiNameContainingIgnoreCaseAndIsActiveTrueOrderByCreatedAtDesc(String keyword);

    List<SharedApi> findByDataCountLessThanEqualAndIsActiveTrueOrderByCreatedAtDesc(Integer dataCount);

    long countByCreatorIdAndIsActiveTrue(String creatorId);
}*/
