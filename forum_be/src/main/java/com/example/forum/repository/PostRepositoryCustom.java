package com.example.forum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.forum.domain.Post;

public interface PostRepositoryCustom {
    //검색 관련 메서드(제목, 내용, 제목+내용)
    
    Page<Post> findByTitleContainingIgnoreCase(String[] keywords, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String[] keywords, Pageable pageable);

    Page<Post> findByTitleOrContentContainingIgnoreCase(String[] keywords, Pageable pageable);
    
} 