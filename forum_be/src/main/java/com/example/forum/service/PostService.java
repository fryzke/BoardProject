package com.example.forum.service;

import com.example.forum.domain.Category;
import com.example.forum.domain.Image;
import com.example.forum.domain.Post;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.PostDto;
import com.example.forum.dto.PostResponseDto;
import com.example.forum.dto.RestPage;
import com.example.forum.repository.ImageRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final ImageRepository imageRepository;
    private final RedisViewCountService redisViewCountService;

    private final StringRedisTemplate stringRedisTemplate;

    private void clearPostsCache() {
        Set<String> keys = stringRedisTemplate.keys("postsCache::*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // 게시글 생성 (생성 시 목록 캐시 전체 무효화)
    public Post createPost(PostDto dto, String userId) {
        if (dto.getTitle() == null || dto.getTitle().trim().length() < 2 || dto.getTitle().trim().length() > 100) {
            throw new IllegalArgumentException("제목은 2자 이상 100자 이하로 입력해주세요.");
        }
        // 본문 검증 (1자 ~ 20,000자)
        if (dto.getContent() == null || dto.getContent().trim().isEmpty() || dto.getContent().length() > 20000) {
            throw new IllegalArgumentException("본문은 1자 이상 20,000자 이하로 입력해주세요.");
        }

        User author = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (author.getRole() != Role.ADMIN && dto.getCategory() == Category.NOTICE) {
            throw new IllegalArgumentException("공지사항은 관리자만 작성할 수 있습니다.");
        }

        Post post = new Post(
                dto.getTitle(),
                dto.getCategory(),
                dto.getContent(),
                author,
                dto.isPinned());

        postRepository.save(post);

        List<Image> unlinkedImages = imageRepository.findAllByAuthorAndPostIsNull(author);
        for (Image image : unlinkedImages) {
            if (dto.getContent().contains(image.getAccessUrl())) {
                image.setPost(post);
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

    // 게시글 삭제 (해당 게시글 캐시 및 목록 캐시 전체 무효화)
    @Caching(evict = {
            @CacheEvict(value = "postCache", key = "#id"),
    })
    public void deletePost(Long id, String loginId) {
        if (loginId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        if (!post.getAuthor().getUserId().equals(loginId)) {
            throw new IllegalArgumentException("본인이 작성한 글만 삭제할 수 있습니다.");
        }

        imageRepository.updateByPostId(id);
        postRepository.delete(post);
        clearPostsCache();
    }

    // 게시글 수정 (해당 게시글 캐시 및 목록 캐시 전체 무효화)
    @Caching(evict = {
            @CacheEvict(value = "postCache", key = "#id"),
    })
    public void editPost(PostDto dto, Long id, String loginId) {
        if (loginId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (dto.getTitle() == null || dto.getTitle().trim().isBlank()) {
            throw new IllegalArgumentException("제목은 비워둘 수 없습니다.");
        }

        if (dto.getCategory() == null) {
            throw new IllegalArgumentException("카테고리는 비워둘 수 없습니다.");

        }
        if (dto.getContent() == null || dto.getContent().trim().isBlank()) {
            throw new IllegalArgumentException("본문은 비워둘 수 없습니다.");
        }

        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));
        if (!post.getAuthor().getUserId().equals(loginId)) {
            throw new IllegalArgumentException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        if (dto.getCategory() == Category.NOTICE && post.getAuthor().getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("공지사항은 관리자만 작성 할 수 있습니다.");
        }

        post.setTitle(dto.getTitle());
        post.setCategory(dto.getCategory());
        post.setContent(dto.getContent());
        post.setPinned(dto.isPinned());

        // 새로 추가된 작성자의 미연결 이미지 연결
        List<Image> unlinkedImages = imageRepository.findAllByAuthorAndPostIsNull(post.getAuthor());
        for (Image image : unlinkedImages) {
            if (dto.getContent().contains(image.getAccessUrl())) {
                image.setPost(post);
            }
        }

        List<Image> savedImages = imageRepository.findAllByPostId(id);
        for (Image image : savedImages) {
            if (!dto.getContent().contains(image.getAccessUrl())) {
                imageRepository.delete(image);
            }
        }

        postRepository.save(post);
        clearPostsCache();
    }
}
