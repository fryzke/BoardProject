package com.example.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRequestDto {
    private String originalName;
    private String storedName;
    private String accessUrl;
    private Long fileSize;
    private String contentType;
}
