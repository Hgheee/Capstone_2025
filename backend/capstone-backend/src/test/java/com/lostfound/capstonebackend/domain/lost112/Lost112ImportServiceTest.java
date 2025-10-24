package com.lostfound.capstonebackend.domain.lost112;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.capstonebackend.config.Lost112Properties;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto;
import com.lostfound.capstonebackend.domain.lostitem.LostItem;
import com.lostfound.capstonebackend.domain.lostitem.LostItemRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class Lost112ImportServiceTest {

    @Mock
    private Lost112ApiService lost112ApiService;

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private Lost112TempRepository lost112TempRepository;

    @Mock
    private MeterRegistry meterRegistry;

    private Lost112ImportService importService;

    @BeforeEach
    void setUp() {
        // `@InjectMocks` requires constructor parameters, so set missing ones manually.
        Lost112Properties properties = new Lost112Properties();
        ObjectMapper objectMapper = new ObjectMapper();
        importService = new Lost112ImportService(
                lost112ApiService,
                lostItemRepository,
                lost112TempRepository,
                properties,
                objectMapper,
                meterRegistry
        );
    }

    @Test
    @DisplayName("LOST112 데이터 수집 - 신규 및 중복 카운트")
    void importLost112DataCountsDuplicates() {
        // Given
        Lost112ItemDto newItem = createItem("A1", "지갑");
        Lost112ItemDto duplicateItem = createItem("A2", "휴대폰");
        given(lost112ApiService.fetchAllLostItems()).willReturn(List.of(newItem, duplicateItem));
        given(lostItemRepository.findByExternalId("A1")).willReturn(Optional.empty());
        given(lostItemRepository.findByExternalId("A2")).willReturn(Optional.of(new LostItem()));
        given(lostItemRepository.save(any(LostItem.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        Lost112ImportService.Lost112ImportResult result = importService.importLost112Data();

        // Then
        assertThat(result.getTotalFetched()).isEqualTo(2);
        assertThat(result.getNewlyCreated()).isEqualTo(1);
        assertThat(result.getDuplicatesSkipped()).isEqualTo(1);

        ArgumentCaptor<LostItem> savedCaptor = ArgumentCaptor.forClass(LostItem.class);
        verify(lostItemRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getExternalId()).isEqualTo("A1");
    }

    private Lost112ItemDto createItem(String atcId, String name) {
        Lost112ItemDto dto = new Lost112ItemDto();
        dto.setAtcId(atcId);
        dto.setFdPrdtNm(name);
        dto.setFdSbjt(name + " 상세");
        dto.setFdYmd("20240101");
        dto.setPrdtClNm("전자");
        dto.setClrNm("검정");
        dto.setDepPlace("서울경찰서");
        dto.setFdFilePathImg("http://image");
        return dto;
    }
}
