/* 공유 기능 비활성화
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

    List<UserSavedApi> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);

    Page<UserSavedApi> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<UserSavedApi> findByUserApiIdAndIsDeletedFalse(String userApiId);

    List<UserSavedApi> findBySharedApiIdAndIsDeletedFalse(String sharedApiId);

    Optional<UserSavedApi> findByUserIdAndSharedApiIdAndIsDeletedFalse(String userId, String sharedApiId);
}*/
