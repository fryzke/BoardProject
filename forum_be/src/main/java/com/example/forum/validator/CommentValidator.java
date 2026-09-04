package com.example.forum.validator;

import org.springframework.stereotype.Component;

import com.example.forum.domain.Comment;
import com.example.forum.domain.Post;
import com.example.forum.dto.CommentRequestDto;

@Component
public class CommentValidator {

    private static final int MAX_COMMENT_DEPTH = 9; // 부모의 최대 depth (자식 포함 최대 10단계)

    /**
     * 댓글 생성 유효성 검증
     */
    public void validateCreate(CommentRequestDto dto, Post post, Comment parentComment, Long postId) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }

        if (parentComment != null) {
            // 다른 게시글의 댓글에 답글 작성 방지
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("다른 게시글의 댓글에는 답글을 작성할 수 없습니다.");
            }

            // 계층(Depth) 제한: 최대 10단계까지 허용
            if (getCommentDepth(parentComment) >= MAX_COMMENT_DEPTH) {
                throw new IllegalArgumentException("답글은 최대 10단계까지만 작성할 수 있습니다.");
            }
        }
    }

    /**
     * 댓글 수정 유효성 검증
     */
    public void validateUpdate(Comment comment, CommentRequestDto dto, Long postId, String loginUserId) {
        validateCommon(comment, postId, loginUserId);

        if (comment.isDeleted()) {
            throw new IllegalArgumentException("삭제된 댓글은 수정할 수 없습니다.");
        }

        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("수정할 댓글 내용을 입력해주세요.");
        }
    }

    /**
     * 댓글 삭제 유효성 검증
     */
    public void validateDelete(Comment comment, Long postId, String loginUserId) {
        validateCommon(comment, postId, loginUserId);

        if (comment.isDeleted()) {
            throw new IllegalArgumentException("이미 삭제된 댓글입니다.");
        }
    }

    private void validateCommon(Comment comment, Long postId, String loginUserId) {
        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("게시글 정보가 일치하지 않습니다.");
        }

        if (loginUserId == null || loginUserId.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (!comment.getAuthor().getUserId().equals(loginUserId)) {
            throw new IllegalArgumentException("해당 댓글에 대한 권한이 없습니다.");
        }
    }

    private int getCommentDepth(Comment comment) {
        int depth = 0;
        Comment current = comment;
        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }
}
