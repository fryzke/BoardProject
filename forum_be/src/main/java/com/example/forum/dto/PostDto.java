package com.example.forum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.forum.domain.Category;
import com.example.forum.validator.annotation.ValidPostContent;
import com.example.forum.validator.annotation.ValidPostTitle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @ValidPostTitle
    private String title;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Category category;

    @NotBlank(message = "본문 내용을 입력해주세요.")
    @ValidPostContent
    private String content;

    @JsonProperty("isPinned")
    private boolean isPinned;
}

