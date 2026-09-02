package com.example.forum.repository;

import com.example.forum.domain.Category;
import com.example.forum.domain.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long>{
    Long countByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByCategory(Category category, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findAll(Pageable pageable);

    //검색 관련 메서드(제목, 내용, 제목+내용)
    Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Post> findByTitleOrContentContainingIgnoreCase(String keyword, Pageable pageable);
}

