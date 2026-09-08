package com.example.forum.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String url;
    private Pagination pagination;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private long totalPosts;
        private long totalComments;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String url, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .url(url)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> ofPage(T data, Page<?> page, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .pagination(Pagination.builder()
                        .currentPage(page.getNumber() + 1)
                        .totalPages(page.getTotalPages())
                        .totalElements(page.getTotalElements())
                        .totalPosts(page.getTotalElements())
                        .totalComments(page.getTotalElements())
                        .build())
                .message(message)
                .build();
    }
}