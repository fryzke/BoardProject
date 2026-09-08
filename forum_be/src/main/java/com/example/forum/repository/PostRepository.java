package com.example.forum.repository;

import com.example.forum.domain.Category;
import com.example.forum.domain.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>{
    Long countByAuthorId(Long authorId);
    long countByIsPinnedTrue();

    @Query(value = "SELECT COUNT(*) FROM posts", nativeQuery = true)
    long countAllIncludingDeleted();

    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByCategory(Category category, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findAll(Pageable pageable);

    //검색 관련 메서드(제목, 내용, 제목+내용)
    Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(
        "SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> findByTitleOrContentContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
}

