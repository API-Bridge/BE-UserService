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

    List<CustomApi> findByUserIdAndDeletedFalse(String userId);

    Page<CustomApi> findByUserIdAndDeletedFalse(String userId, Pageable pageable);

    Optional<CustomApi> findByApiIdAndDeletedFalse(String apiId);

    List<CustomApi> findByUserIdAndDataCountLessThanEqualAndDeletedFalse(String userId, Integer dataCount);
}