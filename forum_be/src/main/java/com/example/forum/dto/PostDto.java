package com.example.forum.dto;

import com.example.forum.domain.Category;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDto {
    private String title;
    private Category category;
    private String content;
    private boolean isPinned;
}
