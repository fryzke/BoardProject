package com.example.forum.service;

import com.example.forum.domain.Category;
import com.example.forum.domain.File;
import com.example.forum.domain.Post;
import com.example.forum.domain.User;
import com.example.forum.dto.PostDto;
import com.example.forum.dto.PostResponseDto;
import com.example.forum.dto.RestPage;
import com.example.forum.event.FileDeleteEvent;
import com.example.forum.repository.FileRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;
import com.example.forum.validator.PostValidator;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final RedisViewCountService redisViewCountService;
    private final PostValidator postValidator;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private void clearPostsCache() {
        Set<String> keys = stringRedisTemplate.keys("postsCache::*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // 게시글 생성 (생성 시 목록 캐시 전체 무효화)
    public Post createPost(PostDto dto, String userId) {
        User author = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        long currentPinnedCount = postRepository.countByIsPinnedTrue();
        postValidator.validateCreate(dto, author, currentPinnedCount);

        Post post = Post.builder()
                .title(dto.getTitle().trim())
                .category(dto.getCategory())
                .content(dto.getContent().trim())
                .author(author)
                .isPinned(dto.isPinned())
                .build();

        postRepository.save(post);

        List<File> unlinkedFiles = fileRepository.findAllByAuthorAndPostIsNull(author);
        for (File file : unlinkedFiles) {
            if (dto.getContent().contains(file.getAccessUrl())) {
                file.setPost(post);
            }
        }

        clearPostsCache();
        return post;
    }

    /*
     * 조회수 증가
     */
    @Async("taskExecutor")
    @Caching(evict = {
            @CacheEvict(value = "postCache", key = "#id"),
    })
    public void increaseViewCount(Long id, String loginId, String clientIp) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        String actualLoginId = (loginId != null && !"anonymousUser".equalsIgnoreCase(loginId) && !loginId.isBlank())
                ? loginId
                : null;

        boolean isAuthor = actualLoginId != null && post.getAuthor().getUserId().equals(actualLoginId);
        if (!isAuthor) {
            String userKey = (actualLoginId != null) ? actualLoginId : clientIp;
            if (userKey != null && redisViewCountService.isFirstView(id, userKey)) {
                post.increaseViewCount();
            }
        }
        clearPostsCache();
    }

    /*
     * 단건 조회 (Redis 캐싱 적용: key = postCache::id)
     */
    @Cacheable(value = "postCache", key = "#id")
    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        return new PostResponseDto(post);
    }

    // 목록 조회 (Redis 캐싱 적용: key = postsCache::category:page:size:sort)
    @Cacheable(value = "postsCache", key = "#category + ':' + #page + ':' + #size + ':' + #sort")
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(int page, int size, String category, String sort) {
        int pageIndex = Math.max(0, page - 1);

        Pageable pageable = sort.equalsIgnoreCase("latest")
                ? PageRequest.of(pageIndex, size, Sort.by(Sort.Order.desc("isPinned"),
                        Sort.Order.desc("createdAt")))
                : PageRequest.of(pageIndex, size, Sort.by(Sort.Order.desc("isPinned"),
                        Sort.Order.desc("viewCount")));

        Page<Post> postPage = "all".equalsIgnoreCase(category)
                ? postRepository.findAll(pageable)
                : postRepository.findByCategory(Category.deserialize(category), pageable);

        List<PostResponseDto> content = postPage.stream().map(PostResponseDto::new).toList();
        return new RestPage<>(content, pageable, postPage.getTotalElements());
    }

    // 게시글 삭제 (해당 게시글 캐시 및 목록 캐시 전체 무효화 + 물리 파일 삭제 이벤트 발행)
    @Caching(evict = {
            @CacheEvict(value = "postCache", key = "#id"),
    })
    public void deletePost(Long id, String loginId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        postValidator.validateDelete(post, loginId);

        List<File> files = fileRepository.findAllByPostId(id);
        fileRepository.updateByPostId(id);
        postRepository.delete(post);
        clearPostsCache();

        // 트랜잭션 성공 후 비동기 물리 파일 삭제 이벤트 발행
        for (File file : files) {
            eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
        }
    }

    // 게시글 수정 (해당 게시글 캐시 및 목록 캐시 전체 무효화)
    @Caching(evict = {
            @CacheEvict(value = "postCache", key = "#id"),
    })
    public void editPost(PostDto dto, Long id, String loginId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        long currentPinnedCount = postRepository.countByIsPinnedTrue();
        postValidator.validateEdit(post, dto, loginId, currentPinnedCount);

        post.update(dto.getTitle().trim(), dto.getCategory(), dto.getContent().trim(), dto.isPinned());

        // 새로 추가된 작성자의 미연결 파일 연결
        List<File> unlinkedFiles = fileRepository.findAllByAuthorAndPostIsNull(post.getAuthor());
        for (File file : unlinkedFiles) {
            if (dto.getContent().contains(file.getAccessUrl())) {
                file.setPost(post);
            }
        }

        // 본문에서 제거된 기존 파일 Soft Delete 및 물리 파일 삭제 이벤트 발행
        List<File> savedFiles = fileRepository.findAllByPostId(id);
        for (File file : savedFiles) {
            if (!dto.getContent().contains(file.getAccessUrl())) {
                fileRepository.delete(file);
                eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
            }
        }

        postRepository.save(post);
        clearPostsCache();
    }
}
