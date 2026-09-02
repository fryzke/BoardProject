package com.example.forum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.forum.domain.Image;
import com.example.forum.domain.User;

public interface ImageRepository extends JpaRepository<Image, Long> {
    @EntityGraph(attributePaths = { "author" })
    List<Image> findAllByPostId(Long postId);

    @EntityGraph(attributePaths = { "author" })
    List<Image> findAllByAuthorAndPostIsNull(User author);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Image i SET i.isDeleted = true, i.deletedAt = CURRENT_TIMESTAMP WHERE i.post.id = :postId")
    void updateByPostId(@Param("postId") Long postId);
}
