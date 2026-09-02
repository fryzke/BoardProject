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
            user1 = new User(
                    "testuser1",
                    passwordEncoder.encode("Test123!"),
                    "테스트유저1",
                    Role.USER,
                    Grade.BRONZE);

            user2 = new User(
                    "testuser2",
                    passwordEncoder.encode("Test123!"),
                    "테스트유저2",
                    Role.USER,
                    Grade.BRONZE);

            admin = new User(
                    "admin001",
                    passwordEncoder.encode("Admin123!"),
                    "관리자",
                    Role.ADMIN,
                    Grade.BRONZE);

            userRepository.save(user1);
            userRepository.save(user2);
            userRepository.save(admin);

            log.info("=== Mock Users Initialized: 2 users + 1 admin ===");
        } else {
            user1 = userRepository.findByUserId("testuser1").orElse(null);
            user2 = userRepository.findByUserId("testuser2").orElse(null);
            admin = userRepository.findByUserId("admin001").orElse(null);
        }

        // 2. 게시글 목데이터 생성 (기존 게시글 삭제 후 55개 생성하여 페이지네이션 및 카테고리 테스트 가능)
        if (user1 != null) {
            postRepository.deleteAll();

            List<Post> mockPosts = new ArrayList<>();
            User[] authors = { user1, user2 != null ? user2 : user1, admin != null ? admin : user1 };
            Category[] categories = Category.values();

            for (int i = 1; i <= 55; i++) {
                User author = authors[i % authors.length];
                Category category = categories[i % categories.length];
                String title = String.format("[%s] 테스트 게시글 %d번째 - %s", category.getName(), i,
                        (i % 5 == 0 ? "Spring Boot 4.1 & React 게시판 프로젝트" : "JPA & Redis CRUD 테스트"));
                String content = String.format(
                        "<p>안녕하세요! 이것은 <strong>%s</strong> 카테고리의 %d번째 테스트 게시글 본문입니다.</p><p>작성자: %s</p><p>페이지네이션 및 카테고리 테스트를 위해 자동 생성된 데이터입니다.</p>",
                        category.getName(), i, author.getUserName());
                
                mockPosts.add(new Post(title, category, content, author, false));
            }

            postRepository.saveAll(mockPosts);
            log.info("=== Mock Posts Initialized: Existing posts deleted and 55 new posts created with categories ===");
        }
    }
}
