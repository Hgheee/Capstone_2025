package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 분실물 정보를 나타내는 메인 엔티티 클래스입니다.
 * 'lost_item' 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "lost_item", indexes = {
        @Index(name = "idx_lost_item_status", columnList = "status"),
        @Index(name = "idx_lost_item_found_date", columnList = "found_date"),
        @Index(name = "idx_lost_item_created_at", columnList = "created_at"),
        @Index(name = "idx_lost_item_category", columnList = "category"),
        @Index(name = "idx_lost_item_external_id", columnList = "external_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LostItem {

    /**
     * 분실물의 고유 식별자(PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 분실물 제목
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 분실물 상세 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 분실물 카테고리
     */
    @Column(length = 50)
    private String category;

    /**
     * 습득 장소
     */
    @Column(length = 200)
    private String location;

    /**
     * 습득 일자
     */
    @Column(name = "found_date")
    private LocalDate foundDate;

    /**
     * 분실물의 현재 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.FOUND;

    /**
     * 외부 시스템(LOST112)의 데이터인 경우, 해당 시스템의 원본 ID
     */
    @Column(name = "external_id", length = 100)
    private String externalId;

    /**
     * 외부 데이터에서 제공되는 조회수입니다.
     */
    @Column(name = "view_count")
    private Integer viewCount;

    /**
     * 외부 데이터에서 수령이 완료된 일시를 나타냅니다.
     */
    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    /**
     * 데이터의 출처 (사용자 직접 등록 또는 외부 시스템)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "datasource", nullable = false)
    @Builder.Default
    private DataSource dataSource = DataSource.USER;

    /**
     * 분실물 색상
     */
    @Column(length = 50)
    private String color;

    /**
     * 보관 장소
     */
    @Column(name = "storage_location", length = 200)
    private String storageLocation;

    /**
     * 분실물 이미지 파일의 URL 경로
     */
    @Column(name = "image_path")
    private String imagePath;

    /**
     * 이 분실물을 등록한 사용자(소유자)입니다.
     * {@link User} 엔티티와 다대일(N:1) 관계를 맺습니다.
     * LAZY 로딩을 사용하여, 실제로 owner 정보가 필요할 때만 데이터베이스에서 조회합니다.
     * LOST112에서 가져온 데이터의 경우 이 값은 null일 수 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    /**
     * 레코드가 생성된 일시 (자동 생성)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 레코드가 마지막으로 수정된 일시 (자동 생성 및 갱신)
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 분실물의 현재 상태를 나타내는 열거형입니다.
     */
    public enum Status {
        FOUND,
        CLAIMED,
        EXPIRED,
        STORED,
        RETURNED,
        DISPOSED
    }

    /**
     * 분실물 데이터의 출처를 나타내는 열거형입니다.
     */
    public enum DataSource {
        USER,
        LOST112,
        SEOUL_LOST
    }

    /**
     * 분실물의 주요 정보를 수정합니다.
     * @param title 새로운 제목
     * @param description 새로운 상세 설명
     * @param category 새로운 카테고리
     * @param location 새로운 습득 장소
     * @param foundDate 새로운 습득일
     * @param color 새로운 색상
     */
    public void updateInfo(String title, String description, String category,
                          String location, LocalDate foundDate, String color) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.foundDate = foundDate;
        this.color = color;
    }

    /**
     * 분실물의 상태를 변경합니다.
     * @param newStatus 변경할 새로운 상태
     */
    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }
}
