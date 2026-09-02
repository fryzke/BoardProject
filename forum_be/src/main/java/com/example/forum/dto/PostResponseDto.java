package com.example.forum.dto;

import java.time.LocalDateTime;

import com.example.forum.domain.Post;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostResponseDto {
    private Long id;
    private String title;
    private String category;
    private String content;
    private String author;
    private boolean isPinned;
    private LocalDateTime createdAt;
    private int viewCount;

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.category = post.getCategory().getName();
        this.content = post.getContent();
        this.isPinned = post.isPinned();
        this.author = post.getAuthor().getUserId();
        this.createdAt = post.getCreatedAt();
        this.viewCount = post.getViewCount();
    }
}
