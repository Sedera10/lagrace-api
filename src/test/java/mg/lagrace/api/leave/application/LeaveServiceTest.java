package mg.lagrace.api.leave.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import mg.lagrace.api.leave.dto.LeaveDtos.Item;
import mg.lagrace.api.leave.dto.LeaveDtos.Overview;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {
    @Mock private NamedParameterJdbcTemplate jdbc;

    private LeaveService service;

    @BeforeEach
    void setUp() {
        service = new LeaveService(null, null, null, null,
                Clock.fixed(Instant.parse("2026-11-23T00:00:00Z"), ZoneId.of("Indian/Antananarivo")), jdbc);
    }

    @Test
    void listWithoutFiltersDoesNotUseUntypedNullSqlParameters() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), ArgumentMatchers.<RowMapper<Item>>any()))
                .thenReturn(List.of());
        when(jdbc.queryForMap(anyString(), any(SqlParameterSource.class))).thenReturn(Map.of(
                "count", 0L, "total_days", 0L, "ongoing_count", 0L, "upcoming_count", 0L));

        service.list(null, null, null, null, null, 0, 20, null);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(query.capture(), any(SqlParameterSource.class), ArgumentMatchers.<RowMapper<Item>>any());
        assertFalse(query.getValue().contains("is null"));

        ArgumentCaptor<String> totalsQuery = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForMap(totalsQuery.capture(), any(SqlParameterSource.class));
        assertFalse(totalsQuery.getValue().contains("is null"));
    }

    @Test
    void overviewConvertsPostgresSerialIdsToLong() throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getLong("employee_id")).thenReturn(7L);
        when(row.getString("employee_name")).thenReturn("Employé Exemple");
        when(row.getObject("current_id")).thenReturn(Integer.valueOf(42));
        when(row.getString("current_name")).thenReturn("Congé payé");
        when(row.getObject("current_start", LocalDate.class)).thenReturn(LocalDate.of(2026, 11, 23));
        when(row.getObject("current_end", LocalDate.class)).thenReturn(LocalDate.of(2026, 11, 24));
        when(jdbc.query(anyString(), anyMap(), ArgumentMatchers.<RowMapper<Overview>>any()))
                .thenAnswer(invocation -> {
                    RowMapper<Overview> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(row, 0));
                });

        List<Overview> result = service.overview(LocalDate.of(2026, 11, 23));

        assertEquals(42L, result.get(0).current().id());
        verify(row).getObject("current_id");
    }
}
