package com.example.forum.dto;

import java.time.LocalDateTime;
import com.example.forum.domain.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor 
@NoArgsConstructor
public class UserResponseDto {
    private String userId;
    private String userName;
    private String role;
    private String grade;
    private Long postCount;
    private Long commentCount;
    private LocalDateTime createdAt;

    public static UserResponseDto of(User user, Long postCount, Long commentCount) {
        return builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .role(user.getRole().name())
                .grade(user.getGrade().name())
                .postCount(postCount)
                .commentCount(commentCount)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
