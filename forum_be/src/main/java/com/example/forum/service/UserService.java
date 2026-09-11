package com.example.forum.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.forum.domain.Grade;
import com.example.forum.domain.User;
import com.example.forum.dto.UserRequestDto;
import com.example.forum.dto.UserResponseDto;
import com.example.forum.repository.CommentRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final com.example.forum.validator.AuthValidator authValidator;

    /*
     * 등급 갱신
     * - 유저 id로 게시글/댓글 수 Count
     * - 기준에 맞는 등급으로 grade 갱신
     */
    @Async("taskExecutor")
    public void updateGrade(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        Long postCount = postRepository.countByAuthorId(user.getId());
        Long commentCount = commentRepository.countByAuthorId(user.getId());

        if (postCount >= Grade.GOLD.getMinPosts() && commentCount >= Grade.GOLD.getMinComments()) {
            user.setGrade(Grade.GOLD);
        } else if (postCount >= Grade.SILVER.getMinPosts() && commentCount >= Grade.SILVER.getMinComments()) {
            user.setGrade(Grade.SILVER);
        } else {
            user.setGrade(Grade.BRONZE);
        }

        userRepository.save(user);
    }

    /*
     * 유저 정보 조회
     * - id에 해당하는 유저 조회
     * - 존재하면 해당 유저의 정보 (+ 게시글, 댓글 수) Dto에 담아 반환
     */

    @Transactional(readOnly = true)
    public UserResponseDto getUserInfo(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Long postCount = postRepository.countByAuthorId(user.getId());
        Long commentCount = commentRepository.countByAuthorId(user.getId());

        return UserResponseDto.of(user, postCount, commentCount);
    }

    /*
     * 유저 정보 수정
     * - id에 해당하는 유저 조회
     * - 존재하면 해당 유저의 정보 업데이트
     */

    public void updateUserInfo(String userId, UserRequestDto dto) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (dto.getCurrentPassword() == null || !passwordEncoder.matches(dto.getCurrentPassword(), user.getUserPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        authValidator.validateUserName(dto.getUserName());

        String encodedPassword = null;
        if (dto.getUserPassword() != null && !dto.getUserPassword().isBlank()) {
            encodedPassword = passwordEncoder.encode(dto.getUserPassword());
        }
        user.update(encodedPassword, dto.getUserName());
    }
}
