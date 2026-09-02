package com.example.forum.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
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

import com.example.forum.dto.CommentRequestDto;
import com.example.forum.dto.CommentResponseDto;
import com.example.forum.service.CommentService;
import com.example.forum.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserService userService;

    /*
     * POST api/comments/{postId}
     * 댓글 작성
     */
    @PostMapping("/{postId}")
    public ResponseEntity<?> createComment(
            @AuthenticationPrincipal String userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto dto) {
        try {
            CommentResponseDto response = commentService.createComment(postId, dto, userId);
            userService.updateGrade(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "댓글이 성공적으로 작성되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * GET api/comments/{postId}
     * 댓글 불러오기
     */
    @GetMapping("/{postId}")
    public ResponseEntity<?> getComments(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit, @PathVariable Long postId) {
        try {
            Page<CommentResponseDto> response = commentService.getCommentsByPost(page, limit, postId);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "댓글을 성공적으로 조회하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * PUT api/comments/{postId}/{commentId}
     * 댓글 수정하기
     */
    @PutMapping("/{postId}/{commentId}")
    public ResponseEntity<?> putComments(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto dto,
            @AuthenticationPrincipal String userId) {
        try {
            CommentResponseDto response = commentService.updateComment(postId, commentId, dto, userId);
            return ResponseEntity.ok(Map.of("success", true, "data", response, "message", "댓글을 성공적으로 수정하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * DELETE api/comments/{postId}/{commentId}
     * 댓글 삭제하기
     */
    @DeleteMapping("/{postId}/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal String userId) {
        try {
            commentService.deleteComment(postId, commentId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "댓글을 성공적으로 삭제하였습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
