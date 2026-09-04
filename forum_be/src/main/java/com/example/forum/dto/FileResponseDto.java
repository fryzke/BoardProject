package com.example.forum.dto;

import java.time.LocalDateTime;

import com.example.forum.domain.File;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileResponseDto {
    private Long id;
    private String originalName;
    private String storedName;
    private String accessUrl;
    private Long fileSize;
    private String contentType;
    private LocalDateTime createdAt;

    public FileResponseDto(File file) {
        this.id = file.getId();
        this.originalName = file.getOriginalName();
        this.storedName = file.getStoredName();
        this.accessUrl = file.getAccessUrl();
        this.fileSize = file.getFileSize();
        this.contentType = file.getContentType();
        this.createdAt = file.getCreatedAt();
    }
}
