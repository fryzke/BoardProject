package com.example.forum.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.forum.domain.Comment;
import com.example.forum.domain.Post;
import com.example.forum.domain.User;
import com.example.forum.dto.CommentRequestDto;
import com.example.forum.dto.CommentResponseDto;
import com.example.forum.repository.CommentRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;
import com.example.forum.validator.CommentValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentValidator commentValidator;

    /**
     * 댓글 작성
     * - 게시글 및 작성자 존재 확인
     * - 대댓글(parentId) 처리 및 깊이 제한
     * - 타 게시글 댓글 부모 설정 방지
     */
    public CommentResponseDto createComment(Long postId, CommentRequestDto dto, String loginUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        User user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Comment parentComment = null;
        if (dto.getParentId() != null) {
            parentComment = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 댓글입니다."));
        }

        commentValidator.validateCreate(dto, post, parentComment, postId);

        Comment comment = Comment.builder()
                .content(dto.getContent().trim())
                .post(post)
                .author(user)
                .parent(parentComment)
                .build();

        commentRepository.save(comment);

        return new CommentResponseDto(comment);
    }

    /**
     * 게시글별 댓글 목록 조회 (트리 구조)
     * - 게시글 존재 확인
     * - 최신순(desc)로 root댓글 조회 후 시간순(asc)로 자식 댓글 조회하여 트리 구조로 계층화
     */
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByPost(int page, int size, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Comment> rootCommentsPage = commentRepository.findByPostIdAndParentIdIsNull(postId, pageable);
        // 1. 루트 댓글만 조회
        List<Comment> rootComments = rootCommentsPage.getContent();

        if (rootComments.isEmpty()) {
            return Page.empty(pageable);
        }
        // 2. 루트 댓글 id 추출
        List<Long> rootIds = rootComments.stream()
                .map(Comment::getId)
                .toList();

        // 3. 해당 id의 대댓글을 조회
        List<Comment> childComments = commentRepository.findByParentIdInOrderByCreatedAtAsc(rootIds);

        // 4. Root 댓글들을 먼저 DTO로 변환하여 Map에 저장
        Map<Long, CommentResponseDto> dtoMap = new HashMap<>();
        List<CommentResponseDto> rootDtos = new ArrayList<>();

        for (Comment root : rootComments) {
            CommentResponseDto dto = new CommentResponseDto(root);
            dtoMap.put(dto.getId(), dto);
            rootDtos.add(dto);
        }

        // 5. 대댓글들을 부모 DTO의 children 리스트에 할당
        for (Comment child : childComments) {
            CommentResponseDto childDto = new CommentResponseDto(child);
            CommentResponseDto parentDto = dtoMap.get(child.getParent().getId());

            if (parentDto != null) {
                parentDto.getChildren().add(childDto);
            }
        }

        return new PageImpl<>(rootDtos, pageable, rootCommentsPage.getTotalElements());
    }

    /**
     * 댓글 수정
     * - 댓글 및 작성자 권한 검증
     * - 삭제된 댓글 수정 방지
     */
    public CommentResponseDto updateComment(Long postId, Long commentId, CommentRequestDto dto, String loginUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        commentValidator.validateUpdate(comment, dto, postId, loginUserId);

        comment.update(dto.getContent().trim());

        return new CommentResponseDto(comment);
    }

    /**
     * 댓글 삭제 (Soft Delete)
     * - 댓글 및 작성자 권한 검증
     * - isDeleted = true 처리 (자식 댓글 보존을 위해 물리 삭제 대신 논리 삭제)
     */
    public void deleteComment(Long postId, Long commentId, String loginUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        commentValidator.validateDelete(comment, postId, loginUserId);

        comment.delete();
    }
}
