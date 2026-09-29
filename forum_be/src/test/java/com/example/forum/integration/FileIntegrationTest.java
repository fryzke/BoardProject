package com.example.forum.integration;

import com.example.forum.domain.Category;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class FileIntegrationTest extends AbstractIntegrationTest {

    @DisplayName("[통합] 파일 업로드 -> 목록 조회 -> 다운로드 -> 삭제 전체 사이클")
    @Test
    void fileUploadAndDownloadLifecycleTest() throws Exception {
        String userId = "file_user1";
        User user = createTestUser(userId, "파일유저", "Password123!", Role.USER, Grade.BRONZE);
        Cookie authCookie = createAccessTokenCookie(userId, Role.USER);

        Post post = Post.builder()
                .title("파일 첨부 게시글")
                .category(Category.TALK)
                .content("<p>파일 첨부 테스트입니다.</p>")
                .author(user)
                .build();
        Post savedPost = postRepository.save(post);
        Long postId = savedPost.getId();

        // 1. 단건 파일 업로드
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Test PDF Content for Integration Testing".getBytes()
        );

        var uploadResult = mockMvc.perform(multipart("/api/files/upload")
                        .file(mockFile)
                        .param("postId", String.valueOf(postId))
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Long fileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        assertThat(fileId).isNotNull().isGreaterThan(0);

        // 2. 게시글별 파일 목록 조회
        mockMvc.perform(get("/api/files/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(fileId))
                .andExpect(jsonPath("$.data[0].originalName").value("test-document.pdf"));

        // 3. 파일 다운로드
        mockMvc.perform(get("/api/files/download/" + fileId))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));

        // 4. 파일 삭제
        mockMvc.perform(delete("/api/files/" + fileId)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
