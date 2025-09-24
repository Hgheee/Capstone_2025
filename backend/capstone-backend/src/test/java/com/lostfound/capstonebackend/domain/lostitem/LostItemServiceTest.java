package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LostItemService 단위 테스트")
class LostItemServiceTest {

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LostItemService lostItemService;

    private User testUser;
    private LostItem testLostItem;
    private LostItemRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        // Reflection으로 필드 설정 (테스트용)
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
            
            var emailField = User.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(testUser, "test@example.com");
            
            var nameField = User.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(testUser, "테스트 사용자");
            
            var passwordField = User.class.getDeclaredField("password");
            passwordField.setAccessible(true);
            passwordField.set(testUser, "encoded-password");
        } catch (Exception e) {
            throw new RuntimeException("테스트 사용자 설정 실패", e);
        }

        testLostItem = LostItem.builder()
                .id(1L)
                .title("잃어버린 지갑")
                .description("검은색 가죽 지갑입니다")
                .category("지갑")
                .location("강남역")
                .foundDate(LocalDate.now().minusDays(1))
                .color("검은색")
                .storageLocation("강남역 분실물 보관소")
                .owner(testUser)
                .dataSource(LostItem.DataSource.USER)
                .status(LostItem.Status.FOUND)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = new LostItemRequest(
                "잃어버린 지갑",
                "검은색 가죽 지갑입니다",
                "지갑",
                "강남역",
                LocalDate.now().minusDays(1),
                "검은색",
                "강남역 분실물 보관소",
                null
        );
    }

    @Test
    @DisplayName("분실물 ID로 조회 성공")
    void findById_Success() {
        // given
        given(lostItemRepository.findById(1L)).willReturn(Optional.of(testLostItem));

        // when
        LostItemResponse result = lostItemService.findById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("잃어버린 지갑");
        assertThat(result.description()).isEqualTo("검은색 가죽 지갑입니다");
    }

    @Test
    @DisplayName("존재하지 않는 분실물 조회 시 예외 발생")
    void findById_NotFound_ThrowsException() {
        // given
        given(lostItemRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lostItemService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("분실물을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("분실물 등록 성공")
    void create_Success() {
        // given
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
        given(lostItemRepository.save(any(LostItem.class))).willReturn(testLostItem);

        // when
        LostItemResponse result = lostItemService.create(testRequest, "test@example.com");

        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("잃어버린 지갑");
        assertThat(result.ownerName()).isEqualTo("테스트 사용자");
        verify(lostItemRepository).save(any(LostItem.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 분실물 등록 시 예외 발생")
    void create_UserNotFound_ThrowsException() {
        // given
        given(userRepository.findByEmail("nonexistent@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lostItemService.create(testRequest, "nonexistent@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("분실물 수정 성공")
    void update_Success() {
        // given
        given(lostItemRepository.findById(1L)).willReturn(Optional.of(testLostItem));
        
        LostItemRequest updateRequest = new LostItemRequest(
                "수정된 제목",
                "수정된 설명",
                "지갑",
                "강남역",
                LocalDate.now().minusDays(1),
                "검은색",
                "강남역 분실물 보관소",
                null
        );

        // when
        LostItemResponse result = lostItemService.update(1L, updateRequest, "test@example.com");

        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("수정된 제목");
        assertThat(result.description()).isEqualTo("수정된 설명");
    }

    @Test
    @DisplayName("LOST112 데이터 수정 시 예외 발생")
    void update_Lost112Data_ThrowsException() {
        // given
        LostItem lost112Item = LostItem.builder()
                .id(1L)
                .title("LOST112 분실물")
                .dataSource(LostItem.DataSource.LOST112)
                .build();
        
        given(lostItemRepository.findById(1L)).willReturn(Optional.of(lost112Item));

        // when & then
        assertThatThrownBy(() -> lostItemService.update(1L, testRequest, "test@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LOST112 데이터는 수정할 수 없습니다");
    }

    @Test
    @DisplayName("다른 사용자의 분실물 수정 시 예외 발생")
    void update_NotOwner_ThrowsException() {
        // given
        given(lostItemRepository.findById(1L)).willReturn(Optional.of(testLostItem));

        // when & then
        assertThatThrownBy(() -> lostItemService.update(1L, testRequest, "other@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("본인이 등록한 분실물만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("분실물 삭제 성공")
    void delete_Success() {
        // given
        given(lostItemRepository.findById(1L)).willReturn(Optional.of(testLostItem));

        // when
        lostItemService.delete(1L, "test@example.com");

        // then
        verify(lostItemRepository).delete(testLostItem);
    }

    @Test
    @DisplayName("전체 분실물 목록 조회 성공")
    void findAll_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<LostItem> page = new PageImpl<>(List.of(testLostItem));
        given(lostItemRepository.findAll(pageable)).willReturn(page);

        // when
        Page<LostItemResponse> result = lostItemService.findAll(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("잃어버린 지갑");
    }

    @Test
    @DisplayName("키워드 검색 성공")
    void searchByKeyword_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<LostItem> page = new PageImpl<>(List.of(testLostItem));
        given(lostItemRepository.findByKeyword("지갑", pageable)).willReturn(page);

        // when
        Page<LostItemResponse> result = lostItemService.searchByKeyword("지갑", pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("잃어버린 지갑");
    }

    @Test
    @DisplayName("빈 키워드로 검색 시 전체 목록 반환")
    void searchByKeyword_EmptyKeyword_ReturnsAll() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<LostItem> page = new PageImpl<>(List.of(testLostItem));
        given(lostItemRepository.findAll(pageable)).willReturn(page);

        // when
        Page<LostItemResponse> result = lostItemService.searchByKeyword("", pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(lostItemRepository).findAll(pageable);
        verify(lostItemRepository, never()).findByKeyword(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("내가 등록한 분실물 목록 조회 성공")
    void findMyItems_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<LostItem> page = new PageImpl<>(List.of(testLostItem));
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
        given(lostItemRepository.findByOwnerId(1L, pageable)).willReturn(page);

        // when
        Page<LostItemResponse> result = lostItemService.findMyItems("test@example.com", pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).ownerName()).isEqualTo("테스트 사용자");
    }

    @Test
    @DisplayName("분실물 상태 변경 성공")
    void updateStatus_Success() {
        // given
        given(lostItemRepository.findById(1L)).willReturn(Optional.of(testLostItem));

        // when
        LostItemResponse result = lostItemService.updateStatus(1L, "CLAIMED", "test@example.com");

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("CLAIMED");
    }

    @Test
    @DisplayName("잘못된 상태값으로 변경 시 예외 발생")
    void updateStatus_InvalidStatus_ThrowsException() {
        // given
        given(lostItemRepository.findById(1L)).willReturn(Optional.of(testLostItem));

        // when & then
        assertThatThrownBy(() -> lostItemService.updateStatus(1L, "INVALID_STATUS", "test@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잘못된 상태값입니다");
    }

    @Test
    @DisplayName("최근 등록된 분실물 조회 성공")
    void findRecentItems_Success() {
        // given
        given(lostItemRepository.findTop10ByOrderByCreatedAtDesc()).willReturn(List.of(testLostItem));

        // when
        List<LostItemResponse> result = lostItemService.findRecentItems();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("잃어버린 지갑");
    }
}