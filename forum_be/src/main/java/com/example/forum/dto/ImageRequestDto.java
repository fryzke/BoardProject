package com.example.forum.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImageRequestDto {
    String originalName;
    String storedName;
    String accessUrl;
    Long fileSize;
    String contentType;
    Long userId;

    
}
