package com.example.forum.repository;

import com.example.forum.domain.Category;
import com.example.forum.domain.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
    Long countByAuthorId(Long authorId);

    long countByIsPinnedTrue();

    @Query(value = "SELECT COUNT(*) FROM posts", nativeQuery = true)
    long countAllIncludingDeleted();

    @EntityGraph(attributePaths = { "author" })
    Page<Post> findByCategory(Category category, Pageable pageable);

    @EntityGraph(attributePaths = { "author" })
    Page<Post> findAll(Pageable pageable);
}
