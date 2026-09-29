package com.example.forum.integration;

import com.example.forum.domain.Category;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.dto.PostDto;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class PostIntegrationTest extends AbstractIntegrationTest {

    @DisplayName("[통합] 게시글 작성 -> 목록 조회 -> 상세 조회 -> 수정 -> 삭제 전체 사이클")
    @Test
    void postCrudLifecycleTest() throws Exception {
        String userId = "post_author1";
        createTestUser(userId, "게시글작성자", "Password123!", Role.USER, Grade.BRONZE);
        Cookie authCookie = createAccessTokenCookie(userId, Role.USER);

        // 1. 게시글 작성
        PostDto createDto = new PostDto();
        createDto.setTitle("통합 테스트 게시글 제목");
        createDto.setCategory(Category.TALK);
        createDto.setContent("<p>통합 테스트 본문 내용입니다.</p>");
        createDto.setPinned(false);
        createDto.setFileIdList(Collections.emptyList());

        mockMvc.perform(post("/api/posts")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // DB에 저장되었는지 확인
        Post savedPost = postRepository.findAll().stream()
                .filter(p -> p.getTitle().equals("통합 테스트 게시글 제목"))
                .findFirst()
                .orElseThrow();
        Long postId = savedPost.getId();
        assertThat(savedPost.getContent()).isEqualTo("<p>통합 테스트 본문 내용입니다.</p>");
        assertThat(savedPost.getAuthor().getUserId()).isEqualTo(userId);

        // 2. 게시글 목록 조회
        mockMvc.perform(get("/api/posts")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("category", "all")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(header().exists("ETag"));

        // 3. 키워드 검색 조회
        mockMvc.perform(get("/api/posts")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("keyword", "통합 테스트")
                        .param("option", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. 게시글 단건 상세 조회
        mockMvc.perform(get("/api/posts/" + postId)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data.title").value("통합 테스트 게시글 제목"));

        // 5. 게시글 수정
        PostDto updateDto = new PostDto();
        updateDto.setTitle("수정된 게시글 제목");
        updateDto.setCategory(Category.INFO);
        updateDto.setContent("<p>수정된 본문 내용입니다.</p>");
        updateDto.setPinned(false);
        updateDto.setFileIdList(Collections.emptyList());

        mockMvc.perform(put("/api/posts/" + postId)
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Post updatedPost = postRepository.findById(postId).orElseThrow();
        assertThat(updatedPost.getTitle()).isEqualTo("수정된 게시글 제목");
        assertThat(updatedPost.getCategory()).isEqualTo(Category.INFO);

        // 6. 게시글 삭제 (Soft Delete & SQLRestriction 검증)
        mockMvc.perform(delete("/api/posts/" + postId)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(postRepository.findById(postId)).isEmpty();
    }
}
