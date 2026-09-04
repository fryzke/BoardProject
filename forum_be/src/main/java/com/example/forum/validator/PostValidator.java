package com.example.forum.validator;

import org.springframework.stereotype.Component;

import com.example.forum.domain.Category;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.PostDto;

@Component
public class PostValidator {

    private static final int MIN_TITLE_LENGTH = 2;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_CONTENT_LENGTH = 1;
    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final int MAX_PINNED_POST_COUNT = 5;

    /**
     * 게시글 작성 유효성 검증
     */
    public void validateCreate(PostDto dto, User author, long currentPinnedCount) {
        validateTitle(dto.getTitle());
        validateContent(dto.getContent());

        if (author.getRole() != Role.ADMIN && dto.getCategory() == Category.NOTICE) {
            throw new IllegalArgumentException("공지사항은 관리자만 작성할 수 있습니다.");
        }

        if (dto.isPinned()) {
            if (author.getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("고정 게시글은 관리자만 설정할 수 있습니다.");
            }
            if (currentPinnedCount >= MAX_PINNED_POST_COUNT) {
                throw new IllegalArgumentException("고정 게시글은 최대 " + MAX_PINNED_POST_COUNT + "개까지만 등록할 수 있습니다.");
            }
        }
    }

    /**
     * 게시글 수정 유효성 검증
     */
    public void validateEdit(Post post, PostDto dto, String loginId, long currentPinnedCount) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (!post.getAuthor().getUserId().equals(loginId)) {
            throw new IllegalArgumentException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        validateTitle(dto.getTitle());

        if (dto.getCategory() == null) {
            throw new IllegalArgumentException("카테고리는 비워둘 수 없습니다.");
        }

        validateContent(dto.getContent());

        if (dto.getCategory() == Category.NOTICE && post.getAuthor().getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("공지사항은 관리자만 작성 할 수 있습니다.");
        }

        if (dto.isPinned()) {
            if (post.getAuthor().getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("고정 게시글은 관리자만 설정할 수 있습니다.");
            }
            if (!post.isPinned() && currentPinnedCount >= MAX_PINNED_POST_COUNT) {
                throw new IllegalArgumentException("고정 게시글은 최대 " + MAX_PINNED_POST_COUNT + "개까지만 등록할 수 있습니다.");
            }
        }
    }

    /**
     * 게시글 삭제 유효성 검증
     */
    public void validateDelete(Post post, String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (!post.getAuthor().getUserId().equals(loginId)) {
            throw new IllegalArgumentException("본인이 작성한 글만 삭제할 수 있습니다.");
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().length() < MIN_TITLE_LENGTH || title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("제목은 " + MIN_TITLE_LENGTH + "자 이상 " + MAX_TITLE_LENGTH + "자 이하로 입력해주세요.");
        }
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty() || content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("본문은 " + MIN_CONTENT_LENGTH + "자 이상 " + MAX_CONTENT_LENGTH + "자 이하로 입력해주세요.");
        }
    }
}
