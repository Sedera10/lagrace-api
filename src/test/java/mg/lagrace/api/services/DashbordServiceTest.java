package mg.lagrace.api.services;

import mg.lagrace.api.dashboard.application.DashbordService;
import mg.lagrace.api.reporting.application.ReportingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos;
import mg.lagrace.api.dashboard.persistence.DashboardNativeRepository;

@ExtendWith(MockitoExtension.class)
class DashbordServiceTest {
    @Mock
    private DashboardNativeRepository repository;

    @Mock
    private ReportingService reportingService;

    private DashbordService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneId.of("Indian/Antananarivo"));
        service = new DashbordService(repository, reportingService, clock);
    }

    @Test
    void countArrivals_returnsTotalAndDone() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        when(repository.arrivals(date)).thenReturn(new DashboardDtos.Progress(12, 9));

        DashboardDtos.Progress result = service.countArrivals(date);

        assertEquals(12L, result.total());
        assertEquals(9L, result.done());
    }

    @Test
    void occupiedRooms_returnsSummary() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        when(repository.occupied(date)).thenReturn(new DashboardDtos.Occupancy(4, 10, new BigDecimal("40.0")));

        DashboardDtos.Occupancy result = service.occupiedRooms(date);

        assertEquals(4L, result.occupied());
        assertEquals(10L, result.total());
        assertEquals(new BigDecimal("40.0"), result.percent());
    }

    @Test
    void amountToCollect_returnsBalance() {
        when(repository.amountToCollect()).thenReturn(new BigDecimal("250000"));

        assertEquals(new BigDecimal("250000"), service.amountToCollect());
    }

    @Test
    void collectedOn_returnsCashAmount() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        when(repository.collected(date)).thenReturn(new BigDecimal("98000"));

        assertEquals(new BigDecimal("98000"), service.collectedOn(date));
    }

    @Test
    void tasksCount_returnsTotalTasks() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        when(repository.taskCount(date)).thenReturn(8L);

        assertEquals(8L, service.tasksCount(date));
    }

    @Test
    void missingDate_usesMadagascarClock() {
        LocalDate today = LocalDate.of(2026, 11, 15);
        when(repository.arrivals(today)).thenReturn(new DashboardDtos.Progress(0, 0));

        assertNotNull(service.countArrivals(null));
    }
}