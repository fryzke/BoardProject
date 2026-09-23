package com.example.forum.repository;

import com.example.forum.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);
    List<User> findAllByIsDeletedIsTrueAndDeletedAtBefore(LocalDateTime threshold);
    boolean existsByUserId(String userId);
    
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM User u WHERE u.userId = :userId", nativeQuery = true)
    void hardDelete(@Param("userId")String userId);

}
