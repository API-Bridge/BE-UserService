package org.example.Usersvc.repository;

import org.example.Usersvc.domain.UserSavedApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSavedApiRepository extends JpaRepository<UserSavedApi, String> {

    List<UserSavedApi> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);

    Page<UserSavedApi> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<UserSavedApi> findByUserApiIdAndDeletedFalse(String userApiId);

    List<UserSavedApi> findBySharedApiIdAndDeletedFalse(String sharedApiId);

    Optional<UserSavedApi> findByUserIdAndSharedApiIdAndDeletedFalse(String userId, String sharedApiId);
}