package com.example.forum.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.example.forum.domain.common.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Table(name = "posts")
@SQLDelete(sql = "UPDATE posts SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<File> files = new ArrayList<>();
    
    @Builder.Default
    @Column(name="is_pinned", nullable = false)
    private boolean isPinned = false; 

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(String title, Category category, String content, boolean isPinned) {
        if (title != null) this.title = title;
        if (category != null) this.category = category;
        if (content != null) this.content = content;
        this.isPinned = isPinned;
    }

    public void changePinned(boolean isPinned) {
        this.isPinned = isPinned;
    }

    public void changeAuthor(User author) {
        this.author = author;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    @PreRemove
    public void onPreRemove() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

}
