package com.example.forum.controller;

import com.example.forum.dto.PostDto;
import com.example.forum.dto.PostResponseDto;
import com.example.forum.service.PostService;
import com.example.forum.service.UserService;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import org.springframework.data.domain.Page;
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
     * POST api/posts
     * 게시글 작성
     */
    @PostMapping
    public ResponseEntity<?> createPost(@AuthenticationPrincipal String userId, @RequestBody PostDto dto) {

        try {
            postService.createPost(dto, userId);
            userService.updateGrade(userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "게시글이 성공적으로 작성되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * GET api/posts
     * 게시글 목록 조회
     * page 0 부터 시작 20개씩 페이지네이션
     */
    @GetMapping
    public ResponseEntity<?> getPosts(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "all")String category, @RequestParam(defaultValue = "latest")String sort) {
        try {
            Page<PostResponseDto> data = postService.getPosts(page, limit, category, sort);
            Map<String, Object> response = Map.of(
                    "success", true,
                    "data", data.getContent(), // 현재 페이지 글 리스트
                    "pagination", Map.of(
                            "currentPage", data.getNumber() + 1, // 0-based -> 1-based
                            "totalPages", data.getTotalPages(), // 총 페이지 수
                            "totalPosts", data.getTotalElements() // 총 글 개수
                    ),
                    "message", "게시글 목록을 조회하였습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * GET api/posts/{id}
     * 게시글 단건 조회 (Redis 기반 24시간 중복 방지 및 작성자 본인 제외)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable Long id,
            @AuthenticationPrincipal String loginId,
            HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            postService.increaseViewCount(id, loginId, clientIp);
            PostResponseDto data = postService.getPost(id);
            return ResponseEntity.ok(Map.of("success", true, "data", data, "message", "게시글을 성공적으로 조회하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * PUT api/posts/{id}
     * 게시글 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> putPost(@PathVariable Long id,
            @RequestBody PostDto dto,
            @AuthenticationPrincipal String loginId) {
        try {
            postService.editPost(dto, id, loginId);
            return ResponseEntity.ok(Map.of("success", true, "message", "게시글을 성공적으로 수정하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * DELETE api/posts/{id}
     * 게시글 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id, @AuthenticationPrincipal String loginId) {
        try {
            postService.deletePost(id, loginId);
            return ResponseEntity.ok(Map.of("success", true, "message", "게시글을 성공적으로 삭제하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
