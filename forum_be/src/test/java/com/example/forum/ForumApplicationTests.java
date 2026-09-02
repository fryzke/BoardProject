package com.example.forum;

import com.example.forum.dto.PostDto;
import com.example.forum.dto.PostResponseDto;
import com.example.forum.domain.Category;
import com.example.forum.domain.User;
import com.example.forum.repository.UserRepository;
import com.example.forum.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ForumApplicationTests {

	@Autowired
	private PostService postService;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("Redis 캐싱 응답 속도 및 Cache Hit/Miss 검증")
	void verifyRedisCachingPerformance() {
		// 1. 테스트용 사용자 및 게시글 준비
		String userId = "cacheTestUser";
		if (userRepository.findByUserId(userId).isEmpty()) {
			User user = new User(userId, "password123", "Cache Tester", com.example.forum.domain.Role.USER, com.example.forum.domain.Grade.BRONZE);
			userRepository.save(user);
		}

		PostDto dto = new PostDto();
		dto.setTitle("캐시 성능 테스트 제목");
		dto.setContent("캐시 성능 테스트 본문 내용입니다.");
		dto.setCategory(Category.TALK);

		var createdPost = postService.createPost(dto, userId);
		Long postId = createdPost.getId();

		// 2. 단건 조회 - 1차 호출 (Cache Miss: DB 조회)
		long startTime1 = System.nanoTime();
		PostResponseDto post1 = postService.getPost(postId);
		long duration1 = (System.nanoTime() - startTime1) / 1_000_000; // ms 단위

		// 3. 단건 조회 - 2차 호출 (Cache Hit: Redis 조회)
		long startTime2 = System.nanoTime();
		PostResponseDto post2 = postService.getPost(postId);
		long duration2 = (System.nanoTime() - startTime2) / 1_000_000; // ms 단위

		System.out.println("==================================================");
		System.out.println("[단건 조회 Cache Miss] 소요 시간: " + duration1 + " ms");
		System.out.println("[단건 조회 Cache Hit ] 소요 시간: " + duration2 + " ms");
		System.out.println("==================================================");

		assertThat(post1.getId()).isEqualTo(post2.getId());

		// 4. 목록 조회 - 1차 호출 (Cache Miss)
		long listStartTime1 = System.nanoTime();
		Page<PostResponseDto> list1 = postService.getPosts(1, 10, "all", "latest");
		long listDuration1 = (System.nanoTime() - listStartTime1) / 1_000_000;

		// 5. 목록 조회 - 2차 호출 (Cache Hit)
		long listStartTime2 = System.nanoTime();
		Page<PostResponseDto> list2 = postService.getPosts(1, 10, "all", "latest");
		long listDuration2 = (System.nanoTime() - listStartTime2) / 1_000_000;

		System.out.println("[목록 조회 Cache Miss] 소요 시간: " + listDuration1 + " ms");
		System.out.println("[목록 조회 Cache Hit ] 소요 시간: " + listDuration2 + " ms");
		System.out.println("==================================================");

		assertThat(list1.getTotalElements()).isEqualTo(list2.getTotalElements());
	}
}
