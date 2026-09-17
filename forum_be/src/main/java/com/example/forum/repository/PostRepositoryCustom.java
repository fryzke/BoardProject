package com.example.forum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.forum.domain.Category;
import com.example.forum.domain.Post;

public interface PostRepositoryCustom {
    Page<Post> searchPosts(Category category, String[] keywords, String option, Pageable pageable);

    Page<Post> findByTitleContainingIgnoreCase(String[] keywords, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String[] keywords, Pageable pageable);

    Page<Post> findByTitleOrContentContainingIgnoreCase(String[] keywords, Pageable pageable);
}
 