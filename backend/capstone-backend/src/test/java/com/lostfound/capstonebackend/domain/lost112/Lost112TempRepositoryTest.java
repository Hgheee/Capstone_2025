package com.lostfound.capstonebackend.domain.lost112;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop"
})
@ActiveProfiles("test")
class Lost112TempRepositoryTest {

    @Autowired
    private Lost112TempRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "create-drop");
    }

    @BeforeEach
    void setUpSchema() {
        entityManager.getEntityManager()
                .createNativeQuery("CREATE TABLE IF NOT EXISTS lost_items_temp (" +
                        "item_id VARCHAR(255) PRIMARY KEY, " +
                        "title VARCHAR(255), " +
                        "found_date DATE, " +
                        "storage_place VARCHAR(255), " +
                        "image_url VARCHAR(512), " +
                        "color VARCHAR(100), " +
                        "description TEXT, " +
                        "category VARCHAR(100), " +
                        "subcategory VARCHAR(100), " +
                        "category_raw VARCHAR(255), " +
                        "created_at TIMESTAMP, " +
                        "updated_at TIMESTAMP)"
                ).executeUpdate();
    }

    @Test
    @DisplayName("전체 개수 계산")
    void countTotalItems() {
        persist("ID1", LocalDate.of(2024, 1, 1));
        persist("ID2", LocalDate.of(2024, 1, 2));

        long count = repository.countTotalItems();
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("생성일 내림차순 조회")
    void findAllOrderByCreatedAtDesc() {
        Lost112TempEntity first = persist("ID1", LocalDate.of(2024, 1, 1));
        Lost112TempEntity second = persist("ID2", LocalDate.of(2024, 1, 2));

        Page<Lost112TempEntity> page = repository.findAllOrderByCreatedAtDesc(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getItemId()).isEqualTo(second.getItemId());
    }

    @Test
    @DisplayName("습득일 기준 조회")
    void findByFoundDateAfter() {
        persist("ID1", LocalDate.of(2024, 1, 1));
        persist("ID2", LocalDate.of(2024, 1, 10));

        Page<Lost112TempEntity> page = repository.findByFoundDateAfter(LocalDate.of(2024, 1, 5), PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getItemId()).isEqualTo("ID2");
    }

    private Lost112TempEntity persist(String id, LocalDate foundDate) {
        Lost112TempEntity entity = Lost112TempEntity.builder()
                .itemId(id)
                .title("물품")
                .foundDate(foundDate)
                .storagePlace("서울")
                .imageUrl("http://image")
                .color("빨강")
                .description("설명")
                .category("카테고리")
                .subcategory(null)
                .categoryRaw("카테고리")
                .build();
        return entityManager.persistAndFlush(entity);
    }
}
