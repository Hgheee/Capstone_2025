package com.lostfound.capstonebackend.lostitem;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LostItem {

    public enum Category { ELECTRONICS, WALLET, UMBRELLA, CLOTHES, ID_CARD, OTHER }
    public enum Status   { REPORTED, FOUND, RETURNED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;     // 분실물 이름
    private String place;     // 분실 장소

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Category category;      // 분류(선택)

    @Column(nullable = true)
    private LocalDate lostDate;     // 분실 날짜(선택, ISO: 2025-09-15)

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Status status;          // 상태(선택: REPORTED/FOUND/RETURNED)

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;     // 상세 설명(선택)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;   // 생성 시각

    @UpdateTimestamp
    private LocalDateTime updatedAt;   // 수정 시각

    @PrePersist
    void prePersist() {
        if (status == null) status = Status.REPORTED; // 기본값
    }
}
