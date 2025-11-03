package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import com.lostfound.capstonebackend.domain.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LostItemServiceTest {

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LostItemService lostItemService;

    @Test
    @DisplayName("전체 분실물 조회")
    void findAll() {
        // Given
        LostItem item = buildItem(1L, "지갑", LostItem.DataSource.USER, "owner@example.com");
        Page<LostItem> items = new PageImpl<>(List.of(item));
        given(lostItemRepository.findAll(any(Pageable.class))).willReturn(items);

        // When
        Page<LostItemResponse> result = lostItemService.findAll(PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("지갑");
    }

    @Test
    @DisplayName("ID로 분실물 조회 - 성공 (N+1 쿼리 방지)")
    void findByIdSuccess() {
        // Given
        LostItem item = buildItem(2L, "휴대폰", LostItem.DataSource.USER, "owner@example.com");
        given(lostItemRepository.findByIdWithOwner(2L)).willReturn(Optional.of(item));

        // When
        LostItemResponse response = lostItemService.findById(2L);

        // Then
        assertThat(response.title()).isEqualTo("휴대폰");
        verify(lostItemRepository).findByIdWithOwner(2L);
        verify(lostItemRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("ID로 분실물 조회 - 실패")
    void findByIdNotFound() {
        given(lostItemRepository.findByIdWithOwner(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> lostItemService.findById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    @DisplayName("분실물 등록 - 성공")
    void createSuccess() {
        // Given
        LostItemRequest request = new LostItemRequest("지갑", "설명", "카테고리", "서울", LocalDate.now(), "검정", "보관", null);
        User owner = buildUser(1L, "user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(owner));
        given(lostItemRepository.save(any(LostItem.class))).willAnswer(invocation -> {
            LostItem saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        // When
        LostItemResponse response = lostItemService.create(request, "user@example.com");

        // Then
        assertThat(response.id()).isEqualTo(10L);
        verify(lostItemRepository).save(any(LostItem.class));
    }

    @Test
    @DisplayName("분실물 등록 - 사용자 없음")
    void createFailsWhenUserMissing() {
        LostItemRequest request = new LostItemRequest("지갑", null, null, null, null, null, null, null);
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> lostItemService.create(request, "missing@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    @DisplayName("분실물 수정 - 성공")
    void updateSuccess() {
        LostItem item = buildItem(5L, "지갑", LostItem.DataSource.USER, "owner@example.com");
        LostItemRequest request = new LostItemRequest("수정된 제목", "설명", "카테고리", "서울", LocalDate.now(), "빨강", null, null);
        given(lostItemRepository.findById(5L)).willReturn(Optional.of(item));

        // When
        LostItemResponse response = lostItemService.update(5L, request, "owner@example.com");

        // Then
        assertThat(response.title()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("분실물 수정 - LOST112 데이터 수정 불가")
    void updateFailsForLost112Source() {
        LostItem item = buildItem(5L, "지갑", LostItem.DataSource.LOST112, null);
        LostItemRequest request = new LostItemRequest("수정", null, null, null, null, null, null, null);
        given(lostItemRepository.findById(5L)).willReturn(Optional.of(item));

        assertThatThrownBy(() -> lostItemService.update(5L, request, "owner@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("분실물 수정 - 등록자 불일치")
    void updateFailsWhenOwnerMismatch() {
        LostItem item = buildItem(5L, "지갑", LostItem.DataSource.USER, "other@example.com");
        LostItemRequest request = new LostItemRequest("수정", null, null, null, null, null, null, null);
        given(lostItemRepository.findById(5L)).willReturn(Optional.of(item));

        assertThatThrownBy(() -> lostItemService.update(5L, request, "owner@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("분실물 삭제 - 성공")
    void deleteSuccess() {
        LostItem item = buildItem(8L, "지갑", LostItem.DataSource.USER, "owner@example.com");
        given(lostItemRepository.findById(8L)).willReturn(Optional.of(item));

        lostItemService.delete(8L, "owner@example.com");

        verify(lostItemRepository).delete(item);
    }

    @Test
    @DisplayName("분실물 삭제 - LOST112 데이터")
    void deleteFailsForLost112() {
        LostItem item = buildItem(8L, "지갑", LostItem.DataSource.LOST112, null);
        given(lostItemRepository.findById(8L)).willReturn(Optional.of(item));

        assertThatThrownBy(() -> lostItemService.delete(8L, "owner@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("내 분실물 조회")
    void findMyItems() {
        User owner = buildUser(12L, "me@example.com");
        LostItem item = buildItem(11L, "우산", LostItem.DataSource.USER, "me@example.com");
        Page<LostItem> page = new PageImpl<>(List.of(item));
        given(userRepository.findByEmail("me@example.com")).willReturn(Optional.of(owner));
        given(lostItemRepository.findByOwnerId(eq(12L), any(Pageable.class))).willReturn(page);

        Page<LostItemResponse> result = lostItemService.findMyItems("me@example.com", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("키워드 검색 - 공백이면 전체 조회")
    void searchByKeywordFallback() {
        Page<LostItem> page = new PageImpl<>(List.of(buildItem(1L, "지갑", LostItem.DataSource.USER, "user@")));
        given(lostItemRepository.findAll(any(Pageable.class))).willReturn(page);

        Page<LostItemResponse> result = lostItemService.searchByKeyword("   ", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("상태 변경 - 성공")
    void updateStatusSuccess() {
        LostItem item = buildItem(13L, "우산", LostItem.DataSource.USER, "owner@example.com");
        given(lostItemRepository.findById(13L)).willReturn(Optional.of(item));

        LostItemResponse response = lostItemService.updateStatus(13L, "CLAIMED", "owner@example.com");
        assertThat(response.status()).isEqualTo("CLAIMED");
    }

    @Test
    @DisplayName("상태 변경 - 잘못된 상태")
    void updateStatusInvalid() {
        LostItem item = buildItem(13L, "우산", LostItem.DataSource.USER, "owner@example.com");
        given(lostItemRepository.findById(13L)).willReturn(Optional.of(item));

        assertThatThrownBy(() -> lostItemService.updateStatus(13L, "INVALID", "owner@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("상태별 최근 분실물 - 잘못된 상태는 빈 리스트")
    void findRecentItemsByStatusInvalid() {
        assertThat(lostItemService.findRecentItemsByStatus("???", 5)).isEmpty();
    }

    @Test
    @DisplayName("기간별 분실물 조회")
    void findByDateRange() {
        Page<LostItem> page = new PageImpl<>(List.of(buildItem(14L, "카드", LostItem.DataSource.USER, "owner")));
        given(lostItemRepository.findByCreatedAtBetween(any(), any(), any(Pageable.class))).willReturn(page);

        Page<LostItemResponse> result = lostItemService.findByDateRange(LocalDate.now().minusDays(1), LocalDate.now(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("내 분실물 상태 통계")
    void getMyStatusStatistics() {
        User owner = buildUser(20L, "owner@example.com");
        List<Object[]> stats = java.util.Collections.singletonList(new Object[]{LostItem.Status.FOUND, 3L});
        given(userRepository.findByEmail("owner@example.com")).willReturn(Optional.of(owner));
        given(lostItemRepository.getStatusStatisticsByOwner(20L)).willReturn(stats);

        List<Object[]> result = lostItemService.getMyStatusStatistics("owner@example.com");
        assertThat(result).hasSize(1);
        assertThat(result.get(0)[1]).isEqualTo(3L);
    }

    private User buildUser(Long id, String email) {
        User user = User.builder()
                .email(email)
                .password("encoded")
                .name("사용자")
                .phone("010-1234-5678")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private LostItem buildItem(Long id, String title, LostItem.DataSource dataSource, String ownerEmail) {
        User owner = null;
        if (ownerEmail != null) {
            owner = buildUser(100L, ownerEmail);
        }
        LostItem item = LostItem.builder()
                .id(id)
                .title(title)
                .description("설명")
                .category("카테고리")
                .location("서울")
                .foundDate(LocalDate.now())
                .status(LostItem.Status.FOUND)
                .externalId("EXT" + id)
                .dataSource(dataSource)
                .color("검정")
                .storageLocation("보관")
                .owner(owner)
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(item, "updatedAt", LocalDateTime.now());
        return item;
    }
}
