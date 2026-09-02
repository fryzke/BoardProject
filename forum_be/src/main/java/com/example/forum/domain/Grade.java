package com.example.forum.domain;

import lombok.Getter;

@Getter
public enum Grade {
    //등급명, 최소글, 최소댓글수
    BRONZE("브론즈", 0, 0),
    SILVER("실버", 5, 10),
    GOLD("골드", 20, 50);

    private final String grade;
    private final int minPosts;
    private final int minComments;

    Grade (String grade, int minPosts, int minComments){
        this.grade = grade;
        this.minPosts = minPosts;
        this.minComments = minComments;
    }
}
