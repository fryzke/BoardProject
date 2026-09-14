package com.example.forum.dto;

import java.time.LocalDateTime;

import java.util.List;

import com.example.forum.domain.File;
import com.example.forum.domain.Post;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("isPinned")
    private boolean isPinned;

    private LocalDateTime createdAt;
    private int viewCount;
    private List<FileResponseDto> files;
    private int fileCount;

    public PostResponseDto(Post post) {
        this(post, null);
    }

    public PostResponseDto(Post post, List<File> files) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.category = post.getCategory().getName();
        this.content = post.getContent();
        this.isPinned = post.isPinned();
        this.author = post.getAuthor().getUserId();
        this.createdAt = post.getCreatedAt();
        this.viewCount = post.getViewCount();
        if (files != null) {
            this.files = files.stream().map(FileResponseDto::new).toList();
            this.fileCount = files.size();
        } else {
            this.files = List.of();
            this.fileCount = 0;
        }
    }
}
