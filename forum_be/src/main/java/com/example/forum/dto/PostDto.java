package com.example.forum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.forum.domain.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Category category;

    @NotBlank(message = "본문 내용을 입력해주세요.")
    @Size(max = 20000, message = "본문은 최대 20,000자 이하로 입력해주세요.")
    private String content;

    @JsonProperty("isPinned")
    private boolean isPinned;
}

