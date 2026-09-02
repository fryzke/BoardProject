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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

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

            // 다른 게시글의 댓글에 답글 작성 방지
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("다른 게시글의 댓글에는 답글을 작성할 수 없습니다.");
            }

            // 계층(Depth) 제한: 최대 10단계까지 허용 (부모 depth가 9 이상이면 추가 생성 불가)
            if (getCommentDepth(parentComment) >= 9) {
                throw new IllegalArgumentException("답글은 최대 10단계까지만 작성할 수 있습니다.");
            }
        }

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
     * 댓글의 계층(Depth) 깊이 계산
     * - 최상위 댓글: depth 0
     * - 1차 답글: depth 1
     * - ... 최대 depth 9 부모 아래 달리는 답글 = depth 10
     */
    private int getCommentDepth(Comment comment) {
        int depth = 0;
        Comment current = comment;
        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    /**
     * 게시글별 댓글 목록 조회 (트리 구조)
     * - 게시글 존재 확인
     * - 최신순(desc)로 root댓글 조회 후 시간순(asc)로 자식 댓글 조회하여 트리 구조로 계층화
     */
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByPost(int pageIndex, int size, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }

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

        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("게시글 정보가 일치하지 않습니다.");
        }

        if (comment.isDeleted()) {
            throw new IllegalArgumentException("삭제된 댓글은 수정할 수 없습니다.");
        }

        if (!comment.getAuthor().getUserId().equals(loginUserId)) {
            throw new IllegalArgumentException("해당 댓글을 수정할 권한이 없습니다.");
        }

        comment.setContent(dto.getContent().trim());

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

        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("게시글 정보가 일치하지 않습니다.");
        }

        if (comment.isDeleted()) {
            throw new IllegalArgumentException("이미 삭제된 댓글입니다.");
        }

        if (!comment.getAuthor().getUserId().equals(loginUserId)) {
            throw new IllegalArgumentException("해당 댓글을 삭제할 권한이 없습니다.");
        }

        comment.setDeleted(true);
    }
}
