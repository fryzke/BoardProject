package com.example.forum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.forum.domain.Category;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDto {
    private String title;
    private Category category;
    private String content;

    @JsonProperty("isPinned")
    private boolean isPinned;
}
