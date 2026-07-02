package com.suyash.smarturl.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "url_mapping",
        indexes = {
                @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
                @Index(name = "idx_original_url", columnList = "originalUrl")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 3000)
    private String originalUrl;

    @Column(nullable = false,unique = true,length = 10)
    private String shortCode;

    @Column(nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime expiryDate;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}