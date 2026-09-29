package com.example.forum.integration;

import com.example.forum.domain.Category;
import com.example.forum.domain.Comment;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.CommentRequestDto;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class CommentIntegrationTest extends AbstractIntegrationTest {

    @DisplayName("[통합] 댓글 및 대댓글 작성 -> 트리 계층 조회 -> 수정 -> 삭제 전체 흐름")
    @Test
    void commentLifecycleAndTreeTest() throws Exception {
        String userId = "comment_user1";
        User user = createTestUser(userId, "댓글작성자", "Password123!", Role.USER, Grade.BRONZE);
        Cookie authCookie = createAccessTokenCookie(userId, Role.USER);

        // 테스트용 게시글 생성
        Post post = Post.builder()
                .title("댓글 테스트용 게시글")
                .category(Category.TALK)
                .content("<p>댓글 테스트 본문입니다.</p>")
                .author(user)
                .build();
        Post savedPost = postRepository.save(post);
        Long postId = savedPost.getId();

        // 1. 부모 댓글 작성 (Depth 0)
        CommentRequestDto parentDto = new CommentRequestDto();
        parentDto.setContent("첫 번째 루트 댓글입니다.");
        parentDto.setParentId(null);

        var parentResult = mockMvc.perform(post("/api/comments/" + postId)
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("첫 번째 루트 댓글입니다."))
                .andReturn();

        Long parentCommentId = objectMapper.readTree(parentResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. 대댓글 작성 (Depth 1, parentId 지정)
        CommentRequestDto childDto = new CommentRequestDto();
        childDto.setContent("첫 번째 댓글에 대한 대댓글입니다.");
        childDto.setParentId(parentCommentId);

        mockMvc.perform(post("/api/comments/" + postId)
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("첫 번째 댓글에 대한 대댓글입니다."));

        // 3. 댓글 목록 계층 트리 조회
        mockMvc.perform(get("/api/comments/" + postId)
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(parentCommentId))
                .andExpect(jsonPath("$.data[0].children").isArray())
                .andExpect(jsonPath("$.data[0].children[0].content").value("첫 번째 댓글에 대한 대댓글입니다."));

        // 4. 댓글 수정
        CommentRequestDto updateDto = new CommentRequestDto();
        updateDto.setContent("수정된 댓글 내용입니다.");
        updateDto.setParentId(null);

        mockMvc.perform(put("/api/comments/" + postId + "/" + parentCommentId)
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Comment updatedComment = commentRepository.findById(parentCommentId).orElseThrow();
        assertThat(updatedComment.getContent()).isEqualTo("수정된 댓글 내용입니다.");

        // 5. 댓글 삭제 (Soft Delete)
        mockMvc.perform(delete("/api/comments/" + postId + "/" + parentCommentId)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Comment deletedComment = commentRepository.findById(parentCommentId).orElseThrow();
        assertThat(deletedComment.isDeleted()).isTrue();
    }
}
