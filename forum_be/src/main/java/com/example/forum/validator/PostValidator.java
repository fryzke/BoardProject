package com.example.forum.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.forum.domain.Category;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.PostDto;

@Component
public class PostValidator {

    @Value("${post.max-pinned-count:5}")
    private int maxPinnedPostCount;

    /**
     * 게시글 작성 비즈니스 검증
     */
    public void validateCreate(PostDto dto, User author, long currentPinnedCount) {
        if (author.getRole() != Role.ADMIN && dto.getCategory() == Category.NOTICE) {
            throw new IllegalArgumentException("공지사항은 관리자만 작성할 수 있습니다.");
        }

        if (dto.isPinned()) {
            if (author.getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("고정 게시글은 관리자만 설정할 수 있습니다.");
            }
            if (currentPinnedCount >= maxPinnedPostCount) {
                throw new IllegalArgumentException("고정 게시글은 최대 " + maxPinnedPostCount + "개까지만 등록할 수 있습니다.");
            }
        }
    }

    /**
     * 게시글 수정 비즈니스 검증
     */
    public void validateEdit(Post post, PostDto dto, String loginId, long currentPinnedCount) {
        validateAuthor(post, loginId, "본인이 작성한 글만 수정할 수 있습니다.");

        if (dto.getCategory() == Category.NOTICE && post.getAuthor().getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("공지사항은 관리자만 작성할 수 있습니다.");
        }

        if (dto.isPinned()) {
            if (post.getAuthor().getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("고정 게시글은 관리자만 설정할 수 있습니다.");
            }
            if (!post.isPinned() && currentPinnedCount >= maxPinnedPostCount) {
                throw new IllegalArgumentException("고정 게시글은 최대 " + maxPinnedPostCount + "개까지만 등록할 수 있습니다.");
            }
        }
    }

    /**
     * 게시글 삭제 비즈니스 검증
     */
    public void validateDelete(Post post, String loginId) {
        validateAuthor(post, loginId, "본인이 작성한 글만 삭제할 수 있습니다.");
    }

    private void validateAuthor(Post post, String loginId, String message) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (!post.getAuthor().getUserId().equals(loginId)) {
            throw new IllegalArgumentException(message);
        }
    }
}

