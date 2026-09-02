package com.example.forum.service;

import org.springframework.stereotype.Service;

import com.example.forum.domain.Grade;
import com.example.forum.domain.User;
import com.example.forum.repository.CommentRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    /*
     * 등급 갱신
     * - 유저 id로 게시글/댓글 수 Count
     * - 기준에 맞는 등급으로 grade 갱신
     */

    public void updateGrade(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
        
        Long postCount = postRepository.countByAuthorId(user.getId());
        Long commentCount = commentRepository.countByAuthorId(user.getId());

        if (postCount >= Grade.SILVER.getMinPosts() && commentCount >= Grade.SILVER.getMinComments()) {
            user.setGrade(Grade.SILVER);
        } else if (postCount >= Grade.GOLD.getMinPosts() && commentCount >= Grade.GOLD.getMinComments()) {
            user.setGrade(Grade.GOLD);
        }

        userRepository.save(user);
    }

}
