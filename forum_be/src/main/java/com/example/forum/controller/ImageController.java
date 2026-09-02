package com.example.forum.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.forum.dto.ImageRequestDto;
import com.example.forum.dto.ImageResponseDto;
import com.example.forum.service.ImageService;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor

public class ImageController {
    private final ImageService imageService;

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    /*
     * POST api/images/upload
     * 이미지 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId", required = false) Long postId,
            @AuthenticationPrincipal String loginUserId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "업로드할 파일이 없습니다."));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이미지 파일만 업로드할 수 있습니다."));
        }

        try {
            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String savedFilename = UUID.randomUUID().toString() + extension;
            File dest = new File(folder.getAbsolutePath() + File.separator + savedFilename);

            file.transferTo(dest);

            String imageUrl = "http://localhost:8080/uploads/images/" + savedFilename;
            ImageRequestDto dto = new ImageRequestDto();
            dto.setOriginalName(originalFilename);
            dto.setAccessUrl(imageUrl);
            dto.setStoredName(savedFilename);
            dto.setContentType(file.getContentType());
            dto.setFileSize(file.getSize());
            
            ImageResponseDto response = imageService.createImage(dto, postId, loginUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response,
                    "url", imageUrl,
                    "message", "이미지가 성공적으로 업로드되었습니다."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "파일 업로드에 실패했습니다."));
        }
    }

    /*
     * GET api/images/{id}
     * 이미지 가져오기
     */

    /*
     * GET api/images/{postId}
     * 이미지 목록 가져오기
     */
    @GetMapping("/{postId}")
    public ResponseEntity<?> getImages(@PathVariable Long postId) {
        try {
            List<ImageResponseDto> response = imageService.getImages(postId);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "이미지를 성공적으로 조회하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * PUT api/images/{imageId}
     * 이미지 수정하기
     */

    @PutMapping("/{imageId}")
    public ResponseEntity<?> putImage(@RequestParam("file") MultipartFile file, @PathVariable Long imageId, @AuthenticationPrincipal String userId) {
              if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "업로드할 파일이 없습니다."));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이미지 파일만 업로드할 수 있습니다."));
        }

        try {
            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String savedFilename = UUID.randomUUID().toString() + extension;
            File dest = new File(folder.getAbsolutePath() + File.separator + savedFilename);

            file.transferTo(dest);

            String imageUrl = "http://localhost:8080/uploads/images/" + savedFilename;
            ImageRequestDto dto = new ImageRequestDto();
            dto.setOriginalName(originalFilename);
            dto.setAccessUrl(imageUrl);
            dto.setStoredName(savedFilename);
            dto.setContentType(file.getContentType());
            dto.setFileSize(file.getSize());

            ImageResponseDto response = imageService.editImage(dto, imageId, userId);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "이미지를 성공적으로 수정하였습니다."));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * DELETE api/images/{imageId}
     * 이미지 삭제
     */

    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable Long imageId, @AuthenticationPrincipal String userId) {
        try {
            imageService.deleteImage(imageId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "이미지를 성공적으로 삭제하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
