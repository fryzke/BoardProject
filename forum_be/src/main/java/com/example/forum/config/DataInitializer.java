package com.example.forum.config;

import com.example.forum.domain.Category;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. 유저 목데이터 생성 (없을 경우)
        User user1;
        User user2;
        User admin;

        if (userRepository.count() == 0) {
            user1 = User.builder()
                    .userId("testuser1")
                    .userPassword(passwordEncoder.encode("Test123!"))
                    .userName("테스트유저1")
                    .role(Role.USER)
                    .grade(Grade.BRONZE)
                    .build();

            user2 = User.builder()
                    .userId("testuser2")
                    .userPassword(passwordEncoder.encode("Test123!"))
                    .userName("테스트유저2")
                    .role(Role.USER)
                    .grade(Grade.BRONZE)
                    .build();

            admin = User.builder()
                    .userId("admin001")
                    .userPassword(passwordEncoder.encode("Admin123!"))
                    .userName("관리자")
                    .role(Role.ADMIN)
                    .grade(Grade.BRONZE)
                    .build();

            userRepository.save(user1);
            userRepository.save(user2);
            userRepository.save(admin);

            log.info("=== Mock Users Initialized: 2 users + 1 admin ===");
        } else {
            user1 = userRepository.findByUserId("testuser1").orElse(null);
            user2 = userRepository.findByUserId("testuser2").orElse(null);
            admin = userRepository.findByUserId("admin001").orElse(null);
        }

        // 2. 게시글 목데이터 생성
        if (user1 != null && postRepository.count() == 0) {
            List<Post> mockPosts = new ArrayList<>();
            User[] authors = { user1, user2 != null ? user2 : user1, admin != null ? admin : user1 };
            Category[] categories = Category.values();

            for (int i = 1; i <= 55; i++) {
                boolean isPinned = (i <= 3);
                User author = (isPinned && admin != null) ? admin : authors[i % authors.length];
                Category category = isPinned ? Category.NOTICE : categories[i % categories.length];
                String title = String.format("[%s] %s테스트 게시글 %d번째 - %s", 
                        category.getName(), 
                        (isPinned ? "[고정] " : ""), 
                        i,
                        (i % 5 == 0 ? "Spring Boot 4.1 & React 게시판 프로젝트" : "JPA & Redis CRUD 테스트"));
                String content = String.format(
                        "<p>안녕하세요! 이것은 <strong>%s</strong> 카테고리의 %d번째 테스트 게시글 본문입니다.%s</p><p>작성자: %s</p><p>페이지네이션 및 카테고리 테스트를 위해 자동 생성된 데이터입니다.</p>",
                        category.getName(), i, (isPinned ? " (상단 고정된 게시글)" : ""), author.getUserName());
                mockPosts.add(Post.builder()
                        .title(title)
                        .category(category)
                        .content(content)
                        .author(author)
                        .isPinned(isPinned)
                        .build());
            }

            postRepository.saveAll(mockPosts);
            log.info("=== Mock Posts Initialized: 55 new posts created (3 pinned) ===");
        } else if (postRepository.count() > 0 && postRepository.countByIsPinnedTrue() == 0) {
            // 이미 게시글이 존재하는 경우 첫 3개 게시글을 고정 게시글로 설정
            List<Post> existingPosts = postRepository.findAll();
            int pinCount = 0;
            for (Post p : existingPosts) {
                if (pinCount < 3) {
                    p.setPinned(true);
                    if (admin != null) {
                        p.setAuthor(admin);
                    }
                    pinCount++;
                } else {
                    break;
                }
            }
            postRepository.saveAll(existingPosts.subList(0, pinCount));
            log.info("=== Existing Posts Updated: 3 posts set to pinned ===");
        }
    }
}
