package com.example.forum.dto;

import com.example.forum.domain.Image;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImageResponseDto {
    private Long id;
    private String originalName;
    private String storedName;
    private String accessUrl;
    private Long fileSize;
    private String contentType;
    private Long postId;
    private String authorUserId;

    public ImageResponseDto(Image image) {
        this.id = image.getId();
        this.originalName = image.getOriginalName();
        this.storedName = image.getStoredName();
        this.accessUrl = image.getAccessUrl();
        this.fileSize = image.getFileSize();
        this.contentType = image.getContentType();
        if (image.getPost() != null) {
            this.postId = image.getPost().getId();
        }
        if (image.getAuthor() != null) {
            this.authorUserId = image.getAuthor().getUserId();
        }
    }
}

