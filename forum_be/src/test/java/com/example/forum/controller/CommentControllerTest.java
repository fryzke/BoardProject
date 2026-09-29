package com.example.forum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.forum.annotation.WithCustomMockUser;
import com.example.forum.config.SecurityConfig;
import com.example.forum.domain.Comment;
import com.example.forum.domain.Post;
import com.example.forum.domain.User;
import com.example.forum.dto.CommentRequestDto;
import com.example.forum.dto.CommentResponseDto;
import com.example.forum.dto.RestPage;
import com.example.forum.security.JwtAuthenticationFilter;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.CommentService;
import com.example.forum.service.RateLimitService;
import com.example.forum.service.UserService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithCustomMockUser
@Import(SecurityConfig.class)
public class CommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean 
    private UserService userService;
    @MockitoBean
    private CommentService commentService;
    @MockitoBean
    private RateLimitService rateLimitService;
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    /* 댓글 작성 API /api/comments/{postId} */
    @DisplayName("댓글 작성 성공")
    @Test
    void createCommentSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        CommentRequestDto request = new CommentRequestDto();
        request.setContent("댓글 본문");

        User author = User.builder().userId("testuser1").build();
        Post post = new Post();

        Comment comment = Comment.builder()
                .content("댓글 본문")
                .author(author)
                .post(post)
                .build();

        CommentResponseDto response = new CommentResponseDto(comment);

        when(commentService.createComment(anyLong(), any(CommentRequestDto.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.author").value("testuser1"))
                .andExpect(jsonPath("$.data.content").value("댓글 본문"));
    }

    /* 댓글 조회 API /api/comments/{postId} */
    @DisplayName("댓글 목록 조회 성공")
    @Test
    void getCommentsSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        List<CommentResponseDto> list = new ArrayList<>();
        User author = User.builder()
                .userId("testuser")
                .userName("테스트유저")
                .build();

        Post post = new Post();

        Comment comment = Comment.builder()
                .content("댓글 본문")
                .author(author)
                .post(post)
                .build();

        CommentResponseDto dto = new CommentResponseDto(comment);
        list.add(dto);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentResponseDto> response = new RestPage<>(list, pageable, list.size());
        
        when(commentService.getCommentsByPost(anyInt(), anyInt(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .param("page", "1")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("댓글 본문"))
                .andExpect(jsonPath("$.data[0].author").value("testuser"))
                .andExpect(jsonPath("$.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    /* 댓글 수정 API /api/comments/{postId}/{commentId} */
    @DisplayName("댓글 수정 성공")
    @Test
    void updateCommentSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        CommentRequestDto request = new CommentRequestDto();
        request.setContent("수정 본문");

        mockMvc.perform(put("/api/comments/1/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글을 성공적으로 수정하였습니다."));
    }

    /* 게시글 삭제 API /api/comments/{postId}/{commentId} */
    @DisplayName("댓글 삭제 성공")
    @Test
    void deleteCommentSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/comments/1/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글을 성공적으로 삭제하였습니다."));
    }

}
