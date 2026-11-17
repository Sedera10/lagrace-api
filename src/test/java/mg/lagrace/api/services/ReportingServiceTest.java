package mg.lagrace.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import mg.lagrace.api.dto.dashboard.DashboardDtos.*;

class ReportingServiceTest {
    private ReportingService service;
    private boolean empty;
    private boolean roomCountReferenceDetected;

    @BeforeEach
    void setUp() {
        empty = false;
        JdbcTemplate jdbc = mock(JdbcTemplate.class, this::answerJdbcCall);
        service = new ReportingService(jdbc);
    }

    @Test
    void revenueByActivity_returnsValuesAndZerosForEmptyPeriod() {
        ActivityReport values = service.revenueByActivity(date(), date(), "day", false);
        assertEquals(new BigDecimal("125.0"), values.series().points().get(0).value());
        assertEquals(7, values.series().points().size());
        assertEquals(new BigDecimal("125.0"), values.total().value());
        assertEquals(new BigDecimal("125.0"), service.revenueByActivity(date(), date(), "day", true)
            .series().points().get(0).previousValue());

        empty = true;
        ActivityReport noValues = service.revenueByActivity(date(), date(), "day", false);
        assertEquals(BigDecimal.ZERO, noValues.series().points().get(0).value());
        assertEquals(BigDecimal.ZERO, noValues.total().value());
    }

    @Test
    void revenueByMonth_returnsTwelveMonthsAndEmptyValues() {
        RevenueByMonth values = service.revenueByMonth(List.of(2025));
        assertEquals(12, values.realized().points().size());
        assertEquals(new BigDecimal("125.0"), values.realized().points().get(0).value());
        assertEquals(new BigDecimal("125.0"), service.revenueByMonth(List.of(2025), true)
            .realized().points().get(0).previousValue());

        empty = true;
        RevenueByMonth noValues = service.revenueByMonth(List.of(2025));
        assertEquals(12, noValues.collected().points().size());
        assertEquals(BigDecimal.ZERO, noValues.collected().points().get(0).value());
    }

    @Test
    void occupancyRate_returnsValueAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("25.0"), service.occupancyRate(date(), date()));
        empty = true;
        assertEquals(BigDecimal.ZERO, service.occupancyRate(date(), date()));
    }

    @Test
    void occupancySeries_returnsPointsAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("25.0"), service.occupancySeries(date(), date(), "day").points().get(0).value());
        empty = true;
        assertEquals(BigDecimal.ZERO, service.occupancySeries(date(), date(), "day").points().get(0).value());
    }

    @Test
    void occupancySeries_definesRoomCountCte() {
        roomCountReferenceDetected = false;
        service.occupancySeries(date(), date(), "day");
        assertEquals(true, roomCountReferenceDetected);
    }

    @Test
    void occupancy_composesCurrentAndPreviousPeriod() {
        OccupancyReport values = service.occupancy(date(), date(), "day", true);
        assertEquals(new BigDecimal("25.0"), values.total().previousValue());
        assertEquals(new BigDecimal("25.0"), values.series().points().get(0).previousValue());

        empty = true;
        assertEquals(BigDecimal.ZERO, service.occupancy(date(), date(), "day", false).total().value());
    }

    @Test
    void cancellationRate_returnsValueAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("25.0"), service.cancellationRate(date(), date()));
        empty = true;
        assertEquals(BigDecimal.ZERO, service.cancellationRate(date(), date()));
    }

    @Test
    void cancellationSeries_returnsPointsAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("25.0"), service.cancellationSeries(date(), date(), "week").points().get(0).value());
        empty = true;
        assertEquals(BigDecimal.ZERO, service.cancellationSeries(date(), date(), "week").points().get(0).value());
    }

    @Test
    void cancellation_composesCurrentAndPreviousPeriod() {
        CancellationReport values = service.cancellation(date(), date(), "month", true);
        assertEquals(new BigDecimal("25.0"), values.total().previousValue());
        assertEquals(new BigDecimal("25.0"), values.series().points().get(0).previousValue());

        empty = true;
        assertEquals(BigDecimal.ZERO, service.cancellation(date(), date(), "month", false).total().value());
    }

    @Test
    void bookingsCountSeries_returnsCountsAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("4"), service.bookingsCountSeries(date(), date(), "day", false)
                .points().get(0).value());
        empty = true;
        assertEquals(BigDecimal.ZERO, service.bookingsCountSeries(date(), date(), "day", false)
                .points().get(0).value());
    }

    @Test
    void bookingRateByRoomType_returnsRateAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("25.0"), service.bookingRateByRoomType(date(), date(), "day", false)
                .get(0).rate());
        assertEquals(new BigDecimal("25.0"), service.bookingRateByRoomType(date(), date(), "day", true)
            .get(0).previousRate());
        empty = true;
        assertEquals(BigDecimal.ZERO, service.bookingRateByRoomType(date(), date(), "day", false)
                .get(0).rate());
    }

    @Test
    void averageStayLength_returnsValueAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("2.5"), service.averageStayLength(date(), date()));
        empty = true;
        assertEquals(BigDecimal.ZERO, service.averageStayLength(date(), date()));
    }

    @Test
    void averageStaySeries_returnsPointsAndZeroForEmptyPeriod() {
        assertEquals(new BigDecimal("2.5"), service.averageStaySeries(date(), date()).points().get(0).value());
        empty = true;
        assertEquals(BigDecimal.ZERO, service.averageStaySeries(date(), date()).points().get(0).value());
    }

    @Test
    void averageStayReport_composesCurrentAndPreviousPeriod() {
        AverageStayReport values = service.averageStayReport(date(), date(), null, true);
        assertEquals(new BigDecimal("2.5"), values.total().previousValue());
        assertEquals(new BigDecimal("2.5"), values.series().points().get(0).previousValue());

        empty = true;
        assertEquals(BigDecimal.ZERO, service.averageStayReport(date(), date(), null, false).total().value());
    }

    @Test
    void bookingsReport_returnsRowsAndEmptyTotals() {
        BookingsReport values = service.bookingsReport(date(), date(), null, "WEBSITE", null, null, 0, 20);
        assertEquals(1, values.items().size());
        assertEquals(1, values.totals().count());

        empty = true;
        BookingsReport noValues = service.bookingsReport(date(), date(), null, null, null, null, 0, 20);
        assertEquals(0, noValues.items().size());
        assertEquals(0, noValues.totals().count());
        assertEquals(BigDecimal.ZERO, noValues.totals().total());
    }

    @Test
    void rejectsInvalidPeriodAndPagination() {
        assertThrows(IllegalArgumentException.class,
                () -> service.occupancyRate(date(), date().minusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.bookingsReport(date(), date(), null, null, null, null, -1, 20));
        assertThrows(IllegalArgumentException.class,
            () -> service.occupancyRate(date(), date().plusYears(5).plusDays(1)));
    }

    private Object answerJdbcCall(InvocationOnMock invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("queryForObject")) {
            if (empty) return BigDecimal.ZERO;
            String sql = invocation.getArgument(0);
            if (sql.contains("AVG(br.nights)")) return new BigDecimal("2.5");
            if (sql.contains("ROUND(100.0")) return new BigDecimal("25.0");
            return new BigDecimal("125.0");
        }
        if (!invocation.getMethod().getName().equals("query")) {
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        }
        String sql = invocation.getArgument(0);
        roomCountReferenceDetected |= sql.contains("room_count AS (");
        Object callback = invocation.getArgument(1);
        if (callback instanceof ResultSetExtractor<?>) {
            if (empty) return new BookingsReport(List.of(), new BookingTotals(0, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO), 0, 20);
            BookingReportRow row = new BookingReportRow(1, "R-1", "Client Test", "WEBSITE", "CONFIRMED",
                    date(), date().plusDays(1), "Chambre", new BigDecimal("125.0"), new BigDecimal("50.0"),
                    new BigDecimal("75.0"), java.time.OffsetDateTime.parse("2026-11-15T12:00:00+03:00"));
            return new BookingsReport(List.of(row), new BookingTotals(1, new BigDecimal("125.0"),
                    new BigDecimal("50.0"), new BigDecimal("75.0")), 0, 20);
        }
        if (sql.contains("GROUP BY activity")) {
            if (empty) return List.of();
            return List.of(new Activity("Chambre", new BigDecimal("125.0"), new BigDecimal("125.0")));
        }
        RowMapper<?> mapper = (RowMapper<?>) callback;
        if (sql.contains("requested_years")) {
            List<Object> rows = new ArrayList<>();
            Object[] arguments = invocation.getArguments();
            List<Integer> years = new ArrayList<>();
            for (int index = 2; index < arguments.length; index++) years.add((Integer) arguments[index]);
            for (Object year : years) {
                for (int month = 1; month <= 12; month++) rows.add(mapRow(mapper, month, sql, (Integer) year));
            }
            return rows;
        }
        if (sql.contains("FROM room_type rt")) return List.of(mapRow(mapper, 1, sql));
        if (sql.contains("bucket_starts") || sql.contains("WITH period AS (SELECT ?::date AS from_date, ?::date AS to_date), months")) {
            return List.of(mapRow(mapper, 1, sql));
        }
        return List.of();
    }

    private Object mapRow(RowMapper<?> mapper, int month, String sql) throws Exception {
        return mapRow(mapper, month, sql, 2025);
    }

    private Object mapRow(RowMapper<?> mapper, int month, String sql, int year) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(anyString())).thenAnswer(invocation -> switch ((String) invocation.getArgument(0)) {
            case "activity" -> "Chambre";
            case "name" -> "Standard";
            case "label" -> month == 1 ? "2025-01" : "2026-01-15";
            default -> "";
        });
        when(resultSet.getBigDecimal(anyString())).thenAnswer(invocation -> {
            if (empty) return BigDecimal.ZERO;
            return switch ((String) invocation.getArgument(0)) {
                case "rate" -> empty ? BigDecimal.ZERO : new BigDecimal("25.0");
                case "value" -> empty ? BigDecimal.ZERO : sql.contains("AVG(br.nights)")
                    ? new BigDecimal("2.5") : sql.contains("COUNT(booking.id_booking)")
                        ? new BigDecimal("4") : new BigDecimal("25.0");
                case "amount", "realized", "invoiced", "collected" -> new BigDecimal("125.0");
                default -> BigDecimal.ZERO;
            };
        });
        when(resultSet.getInt("year")).thenReturn(year);
        when(resultSet.getInt("month")).thenReturn(month);
        when(resultSet.getLong("nights_reserved")).thenReturn(empty ? 0L : 5L);
        when(resultSet.getLong("nights_available")).thenReturn(empty ? 0L : 20L);
        return mapper.mapRow(resultSet, month - 1);
    }

    private LocalDate date() {
        return LocalDate.of(2026, 11, 15);
    }
}