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

    @Value("${post.min-title-length:2}")
    private int minTitleLength;

    @Value("${post.max-title-length:100}")
    private int maxTitleLength;

    @Value("${post.min-content-length:1}")
    private int minContentLength;

    @Value("${post.max-content-length:20000}")
    private int maxContentLength;

    /**
     * 게시글 작성 비즈니스 검증
     */
    public void validateCreate(PostDto dto, User author, long currentPinnedCount) {
        validatePostFields(dto);

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
        validatePostFields(dto);

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

    private void validatePostFields(PostDto dto) {
        if (dto == null) {
            return;
        }
        if (dto.getTitle() != null) {
            int len = dto.getTitle().trim().length();
            if (len < minTitleLength || len > maxTitleLength) {
                throw new IllegalArgumentException(String.format("제목은 %d자 이상 %d자 이하로 입력해주세요.", minTitleLength, maxTitleLength));
            }
        }
        if (dto.getContent() != null) {
            int len = dto.getContent().trim().length();
            if (len < minContentLength || len > maxContentLength) {
                throw new IllegalArgumentException(String.format("본문은 %d자 이상 %,d자 이하로 입력해주세요.", minContentLength, maxContentLength));
            }
        }
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

