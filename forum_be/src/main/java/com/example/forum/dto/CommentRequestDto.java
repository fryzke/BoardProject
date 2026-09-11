package com.example.forum.dto;

import com.example.forum.validator.annotation.ValidCommentContent;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentRequestDto {
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @ValidCommentContent
    private String content;

    private Long parentId;
}

