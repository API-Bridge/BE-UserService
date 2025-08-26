/* CustomAPI 기능 분리 - Custom API Service로 이관
package org.example.Usersvc.repository;

import org.example.Usersvc.domain.CustomApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomApiRepository extends JpaRepository<CustomApi, String> {

    List<CustomApi> findByUserId(String userId);

    Page<CustomApi> findByUserId(String userId, Pageable pageable);

    Optional<CustomApi> findByCustomApiId(String customApiId);

    List<CustomApi> findByUserIdOrderByCreatedAtDesc(String userId);

    List<CustomApi> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    long countByUserId(String userId);
}*/
