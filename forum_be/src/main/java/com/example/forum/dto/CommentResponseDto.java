package com.example.forum.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.forum.domain.Comment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private boolean isDeleted;

    private List<CommentResponseDto> children = new ArrayList<>();

    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
        this.author = comment.getAuthor().getUserId();
        this.createdAt = comment.getCreatedAt();
        this.isDeleted = comment.isDeleted();
        this.children = new ArrayList<>();
    }

}
