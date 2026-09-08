package com.example.forum.controller;

import com.example.forum.dto.PostDto;
import com.example.forum.dto.PostResponseDto;
import com.example.forum.dto.common.ApiResponse;
import com.example.forum.service.PostService;
import com.example.forum.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserService userService;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /*
     * POST /api/posts
     * 게시글 작성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createPost(
            @AuthenticationPrincipal String userId,
            @RequestBody @Valid PostDto dto) {
        postService.createPost(dto, userId);
        userService.updateGrade(userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 성공적으로 작성되었습니다."));
    }

    /*
     * GET /api/posts
     * 게시글 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "latest") String sort) {
        Page<PostResponseDto> data = postService.getPosts(page, limit, category, sort);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(ApiResponse.ofPage(data.getContent(), data, "게시글 목록을 조회하였습니다."));
    }

    /*
     * GET /api/posts/{id}
     * 게시글 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal String loginId,
            HttpServletRequest request) {
        String clientIp = getClientIp(request);
        postService.increaseViewCount(id, loginId, clientIp);
        PostResponseDto data = postService.getPost(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(ApiResponse.success(data, "게시글을 성공적으로 조회하였습니다."));
    }

    /*
     * PUT /api/posts/{id}
     * 게시글 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> putPost(
            @PathVariable Long id,
            @RequestBody @Valid PostDto dto,
            @AuthenticationPrincipal String loginId) {
        postService.editPost(dto, id, loginId);
        return ResponseEntity.ok(ApiResponse.success("게시글을 성공적으로 수정하였습니다."));
    }

    /*
     * DELETE /api/posts/{id}
     * 게시글 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal String loginId) {
        postService.deletePost(id, loginId);
        return ResponseEntity.ok(ApiResponse.success("게시글을 성공적으로 삭제하였습니다."));
    }
}
