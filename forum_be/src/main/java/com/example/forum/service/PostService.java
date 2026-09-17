package com.example.forum.service;

import com.example.forum.domain.Category;
import com.example.forum.domain.File;
import com.example.forum.domain.Post;
import com.example.forum.domain.User;
import com.example.forum.dto.PostDto;
import com.example.forum.dto.PostListResponseDto;
import com.example.forum.dto.PostResponseDto;
import com.example.forum.dto.RestPage;
import com.example.forum.event.FileDeleteEvent;
import com.example.forum.repository.FileRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;
import com.example.forum.utils.HtmlUtils;
import com.example.forum.validator.PostValidator;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final RedisViewCountService redisViewCountService;
    private final PostValidator postValidator;
    private final ApplicationEventPublisher eventPublisher;

    // 게시글 생성
    public Post createPost(PostDto dto, String userId) {
        User author = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        long currentPinnedCount = postRepository.countByIsPinnedTrue();
        postValidator.validateCreate(dto, author, currentPinnedCount);

        Post post = Post.builder()
                .title(dto.getTitle().trim())
                .category(dto.getCategory())
                .content(dto.getContent().trim())
                .plainContent(HtmlUtils.removeTag(dto.getContent()))
                .author(author)
                .isPinned(dto.isPinned())
                .build();

        postRepository.save(post);

        fileService.deleteUnlinkedFile(post, dto);

        return post;
    }

    /*
     * 조회수 증가
     */
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
    }

    /*
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        List<File> files = fileRepository.findAllByPostId(id);
        return new PostResponseDto(post, files);
    }

    // 목록 조회
    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getPosts(int page, int size, String category, String keyword, String option,
            String sort) {
        int pageIndex = Math.max(0, page - 1);

        Sort.Order pinOrder = Sort.Order.desc("isPinned");
        Sort sortOrder;
        if (sort != null && (sort.equalsIgnoreCase("popular") || sort.equalsIgnoreCase("views") || sort.equalsIgnoreCase("viewCount"))) {
            sortOrder = Sort.by(pinOrder, Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"));
        } else {
            sortOrder = Sort.by(pinOrder, Sort.Order.desc("createdAt"));
        }

        Pageable pageable = PageRequest.of(pageIndex, size, sortOrder);
        Page<Post> postPage = searchPosts(category, keyword, option, pageable);

        List<PostListResponseDto> content = postPage.stream().map(PostListResponseDto::new).toList();
        return new RestPage<>(content, pageable, postPage.getTotalElements());
    }

    private Page<Post> searchPosts(String category, String keyword, String option, Pageable pageable) {
        String[] searchKeywords = (keyword != null && !keyword.isBlank())
                ? tokenize(keyword)
                : null;

        // 검색 시에는 전체 게시글을 대상으로 검색, 일반 목록 조회 시에만 카테고리 필터 적용
        Category targetCategory = (searchKeywords == null && category != null && !"all".equalsIgnoreCase(category))
                ? Category.deserialize(category)
                : null;

        return postRepository.searchPosts(targetCategory, searchKeywords, option, pageable);
    }

    private String[] tokenize(String keyword){
        if(keyword == null || keyword.isBlank()){
            return new String[0];
        }
        return keyword.trim().replaceAll("\\s+"," ").split(" ");
    }

    // 게시글 삭제 (물리 파일 삭제 이벤트 발행)
    public void deletePost(Long id, String loginId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        postValidator.validateDelete(post, loginId);

        List<File> files = fileRepository.findAllByPostId(id);
        fileRepository.updateByPostId(id);
        postRepository.delete(post);

        // 트랜잭션 성공 후 비동기 물리 파일 삭제 이벤트 발행
        for (File file : files) {
            eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
        }
    }

    // 게시글 수정
    public void editPost(PostDto dto, Long id, String loginId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        long currentPinnedCount = postRepository.countByIsPinnedTrue();
        postValidator.validateEdit(post, dto, loginId, currentPinnedCount);

        post.update(dto.getTitle().trim(), dto.getCategory(), dto.getContent().trim(), dto.isPinned());

        // 새로 추가된 작성자의 미연결 파일 연결
        fileService.deleteUnlinkedFile(post, dto);

        // 본문 및 첨부파일 목록에서 제거된 기존 파일 Soft Delete 및 물리 파일 삭제 이벤트 발행
        List<File> savedFiles = fileRepository.findAllByPostId(id);
        for (File file : savedFiles) {
            boolean inContent = dto.getContent() != null && dto.getContent().contains(file.getAccessUrl());
            boolean inIdList = dto.getFileIdList() != null && dto.getFileIdList().contains(file.getId());
            if (!inContent && !inIdList) {
                fileRepository.delete(file);
                eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
            }
        }

        postRepository.save(post);
    }
}
