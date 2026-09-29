package com.example.forum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.forum.annotation.WithCustomMockUser;
import com.example.forum.config.SecurityConfig;
import com.example.forum.domain.Category;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.PostDto;
import com.example.forum.dto.PostListResponseDto;
import com.example.forum.dto.PostResponseDto;
import com.example.forum.dto.RestPage;
import com.example.forum.security.JwtAuthenticationFilter;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.PostService;
import com.example.forum.service.RateLimitService;
import com.example.forum.service.UserService;
import com.example.forum.utils.HtmlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithCustomMockUser
@Import(SecurityConfig.class)
public class PostControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PostService postService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private RateLimitService rateLimitService;
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    /* 게시글 작성 API /api/posts */
    @DisplayName("게시글 작성 성공")
    @Test
    void createPostSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        PostDto request = new PostDto();
        request.setCategory(Category.TALK);
        request.setContent("본문");
        request.setTitle("제목");
        request.setPinned(false);
        request.setFileIdList(null);

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글이 성공적으로 작성되었습니다."));
    }

    /* 게시글 조회 API /api/posts */
    @DisplayName("게시글 조회 성공 : 단건")
    @Test
    void getPostSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        User author = User.builder()
                .userId("testuser1")
                .userName("테스트유저1")
                .userPassword(passwordEncoder.encode("Test123!"))
                .role(Role.USER)
                .grade(Grade.BRONZE)
                .build();

        Post post = Post.builder()
                .title("제목")
                .category(Category.TALK)
                .content("본문")
                .plainContent(HtmlUtils.removeTag("본문"))
                .author(author)
                .build();
        PostResponseDto response = new PostResponseDto(post);

        when(postService.getPost(any(Long.class))).thenReturn(response);

        mockMvc.perform(get("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.author").value("testuser1"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.category").value("자유"))
                .andExpect(jsonPath("$.data.content").value("본문"));
    }

    @DisplayName("게시글 조회 성공 : 여러건")
    @Test
    void getPostsSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);
        List<PostListResponseDto> list = new ArrayList<>();
        User author = User.builder()
                .userId("testuser")
                .userName("테스트유저")
                .role(Role.USER)
                .grade(Grade.BRONZE)
                .build();

        Post post = Post.builder()
                .title("제목")
                .category(Category.TALK)
                .content("본문")
                .plainContent(HtmlUtils.removeTag("본문"))
                .author(author)
                .build();

        PostListResponseDto dto = new PostListResponseDto(post);
        list.add(dto);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("createdAt")));
        Page<PostListResponseDto> response = new RestPage<>(list, pageable, list.size());
        when(postService.getPosts(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/posts")
                .param("page", "1")
                .param("limit", "20")
                .param("sort", "latest")
                .param("category", "all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 목록을 조회하였습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("제목"))
                .andExpect(jsonPath("$.data[0].category").value("자유"))
                .andExpect(jsonPath("$.data[0].author").value("testuser"))
                .andExpect(jsonPath("$.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    /* 게시글 수정 API /api/posts/{id} */
    @DisplayName("게시글 수정 성공")
    @Test
    void updatePostSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        Post request = Post.builder()
                .title("수정된 제목")
                .category(Category.TALK)
                .content("수정된 본문")
                .plainContent(HtmlUtils.removeTag("수정된 본문"))
                .build();

        mockMvc.perform(put("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글을 성공적으로 수정하였습니다."));
    }

    /* 게시글 삭제 API /api/posts/{id} */
    @DisplayName("게시글 삭제 성공")
    @Test
    void deletePostSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글을 성공적으로 삭제하였습니다."));
    }
}
