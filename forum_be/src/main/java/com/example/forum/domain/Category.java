package com.example.forum.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    NOTICE(1, "공지사항"),
    TALK(2, "자유"),
    INFO(3, "정보 공유"),
    ASK(4, "질문"),
    REVIEW(5, "후기");

    private final int id;
    @JsonValue
    private final String name;

    @JsonCreator(mode = Mode.DELEGATING)
    public static Category deserialize(String value) {
        if (value == null) {
            return null;
        }
        for (Category category : Category.values()) {
            if (category.getName().equals(value) || category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }

        throw new IllegalArgumentException("해당 카테고리가 존재하지 않습니다.");
    }
}
