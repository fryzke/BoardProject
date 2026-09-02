package com.example.forum.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.forum.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = { "author" })
    List<Comment> findByPostIdOrderByIdAsc(Long postId);

    Long countByAuthorId(Long authorId);

    @EntityGraph(attributePaths = { "author" })
    Page<Comment> findByPostIdAndParentIdIsNull(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = { "author" })
    List<Comment> findByParentIdInOrderByCreatedAtAsc(List<Long> parentIds);
}
