package com.example.forum;

import com.example.forum.domain.Category;
import com.example.forum.domain.User;
import com.example.forum.dto.PostDto;
import com.example.forum.repository.UserRepository;
import com.example.forum.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ForumApplicationTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private PostService postService;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.addFilters(new ShallowEtagHeaderFilter())
				.build();
	}

	@Test
	@DisplayName("브라우저 HTTP Header (ETag, If-None-Match, 304 Not Modified) 캐싱 동작 검증")
	void verifyHttpHeaderCachingPerformance() throws Exception {
		// 1. 테스트용 사용자 및 게시글 준비
		String userId = "etagTestUser";
		if (userRepository.findByUserId(userId).isEmpty()) {
			User user = User.builder()
					.userId(userId)
					.userPassword("password123")
					.userName("ETag Tester")
					.role(com.example.forum.domain.Role.USER)
					.grade(com.example.forum.domain.Grade.BRONZE)
					.build();
			userRepository.save(user);
		}

		PostDto dto = new PostDto();
		dto.setTitle("HTTP Header 캐시 테스트 제목");
		dto.setContent("HTTP Header 캐시 테스트 본문 내용입니다.");
		dto.setCategory(Category.TALK);

		var createdPost = postService.createPost(dto, userId);
		Long postId = createdPost.getId();

		// 2. 단건 조회 - 1차 요청 (ETag 헤더 수신 및 200 OK)
		MvcResult result1 = mockMvc.perform(get("/api/posts/" + postId))
				.andExpect(status().isOk())
				.andExpect(header().exists("ETag"))
				.andExpect(header().string("Cache-Control", "no-cache, must-revalidate"))
				.andReturn();

		String etag = result1.getResponse().getHeader("ETag");
		assertThat(etag).isNotNull().isNotBlank();

		// 3. 단건 조회 - 2차 요청 (If-None-Match 헤더 첨부 -> 304 Not Modified & 빈 바디 반환)
		MvcResult result2 = mockMvc.perform(get("/api/posts/" + postId)
						.header("If-None-Match", etag))
				.andExpect(status().isNotModified())
				.andReturn();

		assertThat(result2.getResponse().getContentAsString()).isEmpty();

		// 4. 목록 조회 - 1차 요청 (ETag 헤더 수신)
		MvcResult listResult1 = mockMvc.perform(get("/api/posts"))
				.andExpect(status().isOk())
				.andExpect(header().exists("ETag"))
				.andReturn();

		String listEtag = listResult1.getResponse().getHeader("ETag");
		assertThat(listEtag).isNotNull().isNotBlank();

		// 5. 목록 조회 - 2차 요청 (If-None-Match 헤더 첨부 -> 304 Not Modified)
		mockMvc.perform(get("/api/posts")
						.header("If-None-Match", listEtag))
				.andExpect(status().isNotModified());
	}
}
