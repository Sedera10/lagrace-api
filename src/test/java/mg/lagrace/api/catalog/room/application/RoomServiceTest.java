package mg.lagrace.api.catalog.room.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mg.lagrace.api.catalog.room.RoomDetail;
import mg.lagrace.api.catalog.room.RoomStatus;
import mg.lagrace.api.catalog.room.dto.room.RoomDetailResponse;
import mg.lagrace.api.catalog.room.persistence.RoomDetailRepository;
import mg.lagrace.api.catalog.room.persistence.RoomRateRepository.ApplicableNightRate;
import mg.lagrace.api.catalog.room.persistence.RoomRepository;
import mg.lagrace.api.catalog.room.persistence.RoomStatusHistoryRepository;
import mg.lagrace.api.catalog.room.persistence.RoomStatusRepository;
import mg.lagrace.api.catalog.room.persistence.RoomTypeRepository;
import mg.lagrace.api.media.application.PhotoService;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
    @Mock private RoomRepository roomRepository;
    @Mock private RoomDetailRepository roomDetailRepository;
    @Mock private RoomStatusRepository roomStatusRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private PhotoService photoService;
    @Mock private RoomStatusHistoryRepository roomStatusHistoryRepository;
    @Mock private RoomTypeService roomTypeService;

    @Test
    void getRoomsUsesRequestedDateForBookingStatusAndNightRate() {
        LocalDate date = LocalDate.of(2026, 12, 4);
        RoomDetail bookedRoom = roomDetail(10, "Chambre réservée", 1, 21);
        RoomDetail formerlyOccupiedRoom = roomDetail(11, "Chambre libre", 2, 21);
        when(roomDetailRepository.findAll()).thenReturn(List.of(bookedRoom, formerlyOccupiedRoom));

        RoomStatus available = status(1L, "Disponible", "#00AA00");
        RoomStatus occupied = status(2L, "Occupée", "#AA0000");
        when(roomStatusRepository.findAll()).thenReturn(List.of(available, occupied));
        when(roomRepository.findBookedRoomIdsOn(date)).thenReturn(List.of(10L));

        ApplicableNightRate rate = new ApplicableNightRate() {
            @Override
            public Long getTypeId() { return 21L; }
            @Override
            public Long getRateId() { return 301L; }
            @Override
            public BigDecimal getAmount() { return new BigDecimal("125000.00"); }
            @Override
            public String getUnitName() { return "Nuit"; }
        };
        when(roomTypeService.getApplicableNightRates(List.of(21L), date))
                .thenReturn(Map.of(21L, rate));

        RoomService service = new RoomService(
                roomRepository,
                roomDetailRepository,
                roomStatusRepository,
                roomTypeRepository,
                photoService,
                roomStatusHistoryRepository,
                roomTypeService);

        List<RoomDetailResponse> responses = service.getRooms(date);

        assertEquals(2, responses.size());
        assertEquals("Occupée", responses.get(0).status());
        assertEquals(2, responses.get(0).statusId());
        assertEquals("Disponible", responses.get(1).status());
        assertEquals(1, responses.get(1).statusId());
        assertEquals(301L, responses.get(0).rateId());
        assertEquals(new BigDecimal("125000.00"), responses.get(0).rate());
        verify(roomTypeService).getApplicableNightRates(List.of(21L), date);
        verify(roomRepository).findBookedRoomIdsOn(date);
    }

    @Test
    void getRoomsDefaultsNullDateToToday() {
        LocalDate before = LocalDate.now();
        when(roomDetailRepository.findAll()).thenReturn(List.of());
        when(roomTypeService.getApplicableNightRates(anyList(), any(LocalDate.class))).thenReturn(Map.of());
        when(roomRepository.findBookedRoomIdsOn(any(LocalDate.class))).thenReturn(List.of());
        when(roomStatusRepository.findAll()).thenReturn(List.of());

        RoomService service = new RoomService(
                roomRepository,
                roomDetailRepository,
                roomStatusRepository,
                roomTypeRepository,
                photoService,
                roomStatusHistoryRepository,
                roomTypeService);

        assertEquals(List.of(), service.getRooms(null));
        LocalDate after = LocalDate.now();
        ArgumentCaptor<LocalDate> requestedDate = ArgumentCaptor.forClass(LocalDate.class);
        verify(roomTypeService).getApplicableNightRates(anyList(), requestedDate.capture());
        verify(roomRepository).findBookedRoomIdsOn(requestedDate.capture());
        assertEquals(requestedDate.getAllValues().get(0), requestedDate.getAllValues().get(1));
        assertFalse(requestedDate.getValue().isBefore(before));
        assertFalse(requestedDate.getValue().isAfter(after));
    }

    private RoomDetail roomDetail(Integer roomId, String ref, Integer statusId, Integer typeId) {
        RoomDetail room = mock(RoomDetail.class);
        when(room.getIdRoom()).thenReturn(roomId);
        when(room.getRefRoom()).thenReturn(ref);
        when(room.getStatusId()).thenReturn(statusId);
        when(room.getTypeId()).thenReturn(typeId);
        return room;
    }

    private RoomStatus status(Long id, String name, String color) {
        RoomStatus status = new RoomStatus();
        status.setIdStatus(id);
        status.setName(name);
        status.setColorCode(color);
        return status;
    }
}
