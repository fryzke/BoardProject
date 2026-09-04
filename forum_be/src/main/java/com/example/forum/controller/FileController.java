package com.example.forum.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.forum.dto.FileRequestDto;
import com.example.forum.dto.FileResponseDto;
import com.example.forum.service.FileService;
import com.example.forum.service.storage.FileStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({ "/api/files", "/api/images" }) // 신규 /api/files 및 기존 프론트엔드 호환용 /api/images 지원
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileStorageService fileStorageService;

    /*
     * POST /api/files/upload 또는 /api/images/upload
     * 파일/이미지 통합 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId", required = false) Long postId,
            @AuthenticationPrincipal String loginUserId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "업로드할 파일이 없습니다."));
        }

        try {
            // 1. 물리 스토리지에 파일 저장
            FileRequestDto storedDto = fileStorageService.storeFile(file);

            // 2. DB에 파일 메타데이터 저장
            FileResponseDto response = fileService.createFile(storedDto, postId, loginUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response,
                    "url", response.getAccessUrl(),
                    "message", "파일이 성공적으로 업로드되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * GET /api/files/{postId} 또는 /api/images/{postId}
     * 게시글에 첨부된 파일 목록 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<?> getFiles(@PathVariable Long postId) {
        try {
            List<FileResponseDto> response = fileService.getFiles(postId);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "파일 목록을 성공적으로 조회하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * PUT /api/files/{fileId} 또는 /api/images/{fileId}
     * 파일 수정 (새 파일로 교체)
     */
    @PutMapping("/{fileId}")
    public ResponseEntity<?> putFile(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long fileId,
            @AuthenticationPrincipal String userId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "업로드할 파일이 없습니다."));
        }

        try {
            FileRequestDto storedDto = fileStorageService.storeFile(file);
            FileResponseDto response = fileService.editFile(storedDto, fileId, userId);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "파일을 성공적으로 수정하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * DELETE /api/files/{fileId} 또는 /api/images/{fileId}
     * 파일 삭제 (논리 삭제 + 물리 파일 비동기 삭제)
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal String userId) {
        try {
            fileService.deleteFile(fileId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "파일을 성공적으로 삭제하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
