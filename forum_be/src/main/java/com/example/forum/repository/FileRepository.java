package com.example.forum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.forum.domain.File;
import com.example.forum.domain.User;

public interface FileRepository extends JpaRepository<File, Long> {
    @EntityGraph(attributePaths = { "author" })
    List<File> findAllByPostId(Long postId);

    @EntityGraph(attributePaths = { "author" })
    List<File> findAllByAuthorAndPostIsNull(User author);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE File f SET f.isDeleted = true, f.deletedAt = CURRENT_TIMESTAMP WHERE f.post.id = :postId")
    void updateByPostId(@Param("postId") Long postId);
}
