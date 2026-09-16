package com.example.forum.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.example.forum.domain.Post;
import com.example.forum.domain.QPost;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findByTitleContainingIgnoreCase(String[] keywords, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        QPost post = QPost.post;
        for (String keyword : keywords) {
            builder.or(post.title.containsIgnoreCase(keyword));
        }

        List<Post> fetch = queryFactory
                .selectFrom(post)
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable, post))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        JPAQuery<Long> count = queryFactory
                .select(post.count())
                .from(post)
                .where(builder);

        return PageableExecutionUtils.getPage(fetch, pageable, count::fetchOne);
    }

    @Override
    public Page<Post> findByContentContainingIgnoreCase(String[] keywords, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        QPost post = QPost.post;
        for (String keyword : keywords) {
            builder.or(post.content.containsIgnoreCase(keyword));
        }

        List<Post> fetch = queryFactory
                .selectFrom(post)
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable, post))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        JPAQuery<Long> count = queryFactory
                .select(post.count())
                .from(post)
                .where(builder);

        return PageableExecutionUtils.getPage(fetch, pageable, count::fetchOne);
    }

    @Override
    public Page<Post> findByTitleOrContentContainingIgnoreCase(String[] keywords, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        QPost post = QPost.post;
        for (String keyword : keywords) {
            builder.or(post.content.containsIgnoreCase(keyword)).or(post.title.containsIgnoreCase(keyword));
        }

        List<Post> fetch = queryFactory
                .selectFrom(post)
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable, post))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        JPAQuery<Long> count = queryFactory
                .select(post.count())
                .from(post)
                .where(builder);

        return PageableExecutionUtils.getPage(fetch, pageable, count::fetchOne);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable, QPost post) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        if (pageable.getSort() != null) {
            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                switch (order.getProperty()) {
                    case "isPinned" -> orders.add(new OrderSpecifier<>(direction, post.isPinned));
                    case "createdAt" -> orders.add(new OrderSpecifier<>(direction, post.createdAt));
                    case "viewCount" -> orders.add(new OrderSpecifier<>(direction, post.viewCount));
                    default -> {}
                }
            }
        }
        return orders.toArray(new OrderSpecifier<?>[0]);
    }
}
