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

    List<SharedApi> findByActiveTrueOrderByCreatedAtDesc();

    Page<SharedApi> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    List<SharedApi> findByCreatorIdAndActiveTrueOrderByCreatedAtDesc(String creatorId);

    Optional<SharedApi> findByOriginalApiIdAndActiveTrue(String originalApiId);

    List<SharedApi> findByApiNameContainingIgnoreCaseAndActiveTrueOrderByCreatedAtDesc(String keyword);

    List<SharedApi> findByDataCountLessThanEqualAndActiveTrueOrderByCreatedAtDesc(Integer dataCount);
}