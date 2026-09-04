package com.example.forum.repository;

import com.example.forum.domain.Category;
import com.example.forum.domain.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long>{
    Long countByAuthorId(Long authorId);
    long countByIsPinnedTrue();

    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByCategory(Category category, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findAll(Pageable pageable);

    //검색 관련 메서드(제목, 내용, 제목+내용)
    Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String keyword, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> findByTitleOrContentContainingIgnoreCase(@org.springframework.data.repository.query.Param("keyword") String keyword, Pageable pageable);
}

