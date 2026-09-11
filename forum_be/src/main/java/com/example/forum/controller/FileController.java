package com.example.forum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.forum.dto.FileResponseDto;
import com.example.forum.dto.common.ApiResponse;
import com.example.forum.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({ "/api/files", "/api/images" }) 
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /*
     * POST /api/files/upload 또는 /api/images/upload
     * 파일/이미지 통합 업로드 (단건 및 다중 업로드 지원)
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<?>> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "postId", required = false) Long postId,
            @AuthenticationPrincipal String loginUserId) {

        if (files != null && !files.isEmpty()) {
            List<FileResponseDto> responses = fileService.uploadFiles(files, postId, loginUserId);
            return ResponseEntity.ok(ApiResponse.success(responses, "파일들이 성공적으로 업로드되었습니다."));
        } else if (file != null && !file.isEmpty()) {
            FileResponseDto response = fileService.uploadFile(file, postId, loginUserId);
            return ResponseEntity.ok(ApiResponse.success(response, response.getAccessUrl(), "파일이 성공적으로 업로드되었습니다."));
        } else {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
    }

    /*
     * GET /api/files/{postId} 또는 /api/images/{postId}
     * 게시글에 첨부된 파일 목록 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<List<FileResponseDto>>> getFiles(@PathVariable Long postId) {
        List<FileResponseDto> response = fileService.getFiles(postId);
        return ResponseEntity.ok(ApiResponse.success(response, "파일 목록을 성공적으로 조회하였습니다."));
    }

    /*
     * PUT /api/files/{fileId} 또는 /api/images/{fileId}
     * 파일 수정 (새 파일로 교체: 메타데이터 수정 -> 2차 검증 -> 물리 파일 저장)
     */
    @PutMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponseDto>> putFile(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long fileId,
            @AuthenticationPrincipal String userId) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        FileResponseDto response = fileService.replaceFile(file, fileId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "파일을 성공적으로 수정하였습니다."));
    }

    /*
     * DELETE /api/files/{fileId} 또는 /api/images/{fileId}
     * 파일 삭제 (논리 삭제 + 물리 파일 비동기 삭제)
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal String userId) {
        fileService.deleteFile(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success("파일을 성공적으로 삭제하였습니다."));
    }

    /*
     * GET /api/files/download/{fileId} 또는 /api/images/download/{fileId}
     * 파일 다운로드 API (파일 ID 기준)
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable Long fileId) {
        FileService.FileDownloadDto downloadDto = fileService.downloadFile(fileId);
        String encodedFileName = org.springframework.web.util.UriUtils.encode(downloadDto.originalName(), java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(downloadDto.contentType() != null ? downloadDto.contentType() : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(downloadDto.resource());
    }

    /*
     * GET /api/files/download 또는 /api/images/download
     * 파일 다운로드 API (storedName 기준)
     */
    @GetMapping("/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFileByName(@RequestParam("storedName") String storedName) {
        FileService.FileDownloadDto downloadDto = fileService.downloadFileByStoredName(storedName);
        String encodedFileName = org.springframework.web.util.UriUtils.encode(downloadDto.originalName(), java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(downloadDto.contentType() != null ? downloadDto.contentType() : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(downloadDto.resource());
    }

    /*
     * POST /api/files/delete-batch 또는 /api/images/delete-batch
     * 다중 파일 삭제 API
     */
    @PostMapping("/delete-batch")
    public ResponseEntity<ApiResponse<Void>> deleteFilesBatch(
            @RequestBody List<Long> fileIds,
            @AuthenticationPrincipal String userId) {
        fileService.deleteFilesBatch(fileIds, userId);
        return ResponseEntity.ok(ApiResponse.success("선택한 파일들이 성공적으로 삭제되었습니다."));
    }
}
