package mg.lagrace.api.reporting.application;

import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.room.RoomType;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos;
import mg.lagrace.api.payment.Payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportingService {
    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);
    private static final long SLOW_QUERY_MILLIS = 500;
    private final JdbcTemplate jdbc;

    public ReportingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private BigDecimal amount(String queryName, String sql, Object... args) {
        return measure(queryName, () -> jdbc.queryForObject(sql, BigDecimal.class, args));
    }

    public List<Activity> revenueByActivity(LocalDate from, LocalDate to) {
        return realizedRevenueByActivity(from, to);
    }

    public ActivityReport revenueByActivity(LocalDate from, LocalDate to, boolean compare) {
        validatePeriod(from, to);
        List<Activity> currentRows = realizedRevenueByActivity(from, to);
        List<Activity> previousRows = compare
                ? realizedRevenueByActivity(previousFrom(from, to), from.minusDays(1)) : List.of();
        Map<String, BigDecimal> current = activityAmounts(currentRows);
        Map<String, BigDecimal> previous = activityAmounts(previousRows);
        BigDecimal total = currentRows.isEmpty() ? BigDecimal.ZERO : zero(currentRows.get(0).total());
        BigDecimal previousTotal = previousRows.isEmpty() ? BigDecimal.ZERO : zero(previousRows.get(0).total());
        List<SeriesPoint> points = new ArrayList<>();
        for (String label : List.of("Chambre", "Espace", "Salle de réunion", "Piscine", "Restaurant", "Services", "Autres")) {
            points.add(new SeriesPoint(label, current.getOrDefault(label, BigDecimal.ZERO),
                compare ? previous.getOrDefault(label, BigDecimal.ZERO) : null));
        }
        return new ActivityReport(new Series(points), new Comparison(total, compare ? previousTotal : null));
    }

    public RevenueByMonth revenueByMonth(List<Integer> requestedYears) {
        return revenueByMonth(requestedYears, false);
    }

    public RevenueByMonth revenueByMonth(List<Integer> requestedYears, boolean compare) {
        if (requestedYears == null || requestedYears.isEmpty() || requestedYears.size() > 6
                || requestedYears.stream().anyMatch(year -> year == null || year < 1 || year > 9998)) {
            throw new IllegalArgumentException("years doit contenir de 1 à 6 années valides.");
        }
        List<Integer> years = requestedYears.stream().distinct().sorted().toList();
        List<Integer> requestedQueryYears = new ArrayList<>(years);
        if (compare) years.stream().filter(year -> year > 1).map(year -> year - 1).forEach(requestedQueryYears::add);
        List<Integer> queryYears = requestedQueryYears.stream().distinct().sorted().toList();
        String yearValues = String.join(", ", java.util.Collections.nCopies(queryYears.size(), "(?)"));
        String sql = """
            WITH requested_years(year) AS (VALUES %s), months AS (
                SELECT year, month, make_date(year, month, 1) AS month_start
                FROM requested_years CROSS JOIN generate_series(1, 12) AS months_of_year(month)
            ), revenue AS (
                SELECT m.year, m.month, SUM(amount) AS amount
                FROM months m
                JOIN LATERAL (
                    SELECT br.unit_price * GREATEST(0, LEAST(br.check_out, (m.month_start + INTERVAL '1 month')::date)
                        - GREATEST(br.check_in, m.month_start)) AS amount
                    FROM booking_room br
                    WHERE br.is_active AND br.check_in < (m.month_start + INTERVAL '1 month')::date
                      AND br.check_out > m.month_start
                    UNION ALL
                    SELECT bs.amount FROM booking_space bs
                    WHERE bs.is_active AND bs.event_date >= m.month_start AND bs.event_date < m.month_start + INTERVAL '1 month'
                    UNION ALL
                    SELECT bm.amount FROM booking_meeting_room bm
                    WHERE bm.is_active AND bm.service_date >= m.month_start AND bm.service_date < m.month_start + INTERVAL '1 month'
                    UNION ALL
                    SELECT bp.amount FROM booking_pool bp
                    WHERE bp.is_active
                      AND (bp.starts_at AT TIME ZONE 'Indian/Antananarivo') >= m.month_start
                      AND (bp.starts_at AT TIME ZONE 'Indian/Antananarivo') < m.month_start + INTERVAL '1 month'
                    UNION ALL
                    SELECT bc.amount FROM booking_charge bc
                    WHERE bc.voided_at IS NULL AND NOT bc.is_deposit
                      AND (bc.charged_at AT TIME ZONE 'Indian/Antananarivo') >= m.month_start
                      AND (bc.charged_at AT TIME ZONE 'Indian/Antananarivo') < m.month_start + INTERVAL '1 month'
                    UNION ALL
                    SELECT bo.amount FROM booking_other bo
                    WHERE bo.voided_at IS NULL
                      AND (bo.created_at AT TIME ZONE 'Indian/Antananarivo') >= m.month_start
                      AND (bo.created_at AT TIME ZONE 'Indian/Antananarivo') < m.month_start + INTERVAL '1 month'
                ) revenue_rows ON TRUE
                GROUP BY m.year, m.month
            ), invoiced AS (
                SELECT m.year, m.month,
                       SUM(CASE WHEN i.invoice_type = 'CREDIT_NOTE' THEN -i.total ELSE i.total END) AS amount
                FROM months m
                JOIN invoice i ON i.issue_date >= m.month_start AND i.issue_date < m.month_start + INTERVAL '1 month'
                JOIN invoice_status status ON status.id_status = i.status_id AND status.code = 'ISSUED'
                GROUP BY m.year, m.month
            ), collected AS (
                SELECT m.year, m.month,
                       SUM(CASE WHEN kind.code = 'REFUND' THEN -p.amount ELSE p.amount END) AS amount
                FROM months m
                JOIN payment p ON (p.paid_at AT TIME ZONE 'Indian/Antananarivo') >= m.month_start
                    AND (p.paid_at AT TIME ZONE 'Indian/Antananarivo') < m.month_start + INTERVAL '1 month'
                JOIN payment_kind kind ON kind.id_kind = p.kind_id
                    AND kind.code IN ('PAYMENT', 'ADVANCE', 'REFUND')
                GROUP BY m.year, m.month
            )
            SELECT m.year, m.month, COALESCE(r.amount, 0) AS realized,
                   COALESCE(i.amount, 0) AS invoiced, COALESCE(c.amount, 0) AS collected
            FROM months m
            LEFT JOIN revenue r USING (year, month)
            LEFT JOIN invoiced i USING (year, month)
            LEFT JOIN collected c USING (year, month)
            ORDER BY m.year, m.month
            """.formatted(yearValues);
        List<RevenueMonthRow> rows = measure("reporting.revenue-by-month", () -> jdbc.query(sql,
                (rs, row) -> new RevenueMonthRow(rs.getInt("year"), rs.getInt("month"),
                        rs.getBigDecimal("realized"), rs.getBigDecimal("invoiced"), rs.getBigDecimal("collected")),
                queryYears.toArray()));
            Map<String, RevenueMonthRow> rowsByMonth = new LinkedHashMap<>();
            rows.forEach(row -> rowsByMonth.put(monthKey(row.year(), row.month()), row));
        return new RevenueByMonth(
                new Series(monthSeries(years, rowsByMonth, compare, RevenueMonthRow::realized)),
                new Series(monthSeries(years, rowsByMonth, compare, RevenueMonthRow::invoiced)),
                new Series(monthSeries(years, rowsByMonth, compare, RevenueMonthRow::collected)));
    }

    public List<RatePoint> occupancySeries(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate start = from;
        LocalDate end = to;
        return measure("reporting.occupancy-series", () -> jdbc.query("""
            WITH days AS (
                SELECT generate_series(?::date, ?::date, '1 day'::interval)::date AS day
            )
            SELECT day,
                   COALESCE(ROUND(100.0 * (
                       SELECT COUNT(*)
                       FROM booking_room br
                       JOIN booking_line_status line_status ON line_status.id_booking_line_status = br.status_id
                       WHERE br.is_active
                         AND line_status.code IN ('CHECK_IN', 'CHECK_OUT')
                         AND br.check_in <= days.day
                         AND br.check_out > days.day
                   ) / NULLIF((SELECT COUNT(*) FROM room WHERE NOT is_deleted), 0), 1), 0) AS value
            FROM days
            ORDER BY day
            """, (rs, row) -> new RatePoint(rs.getDate("day").toLocalDate(), rs.getBigDecimal("value")), start, end));
    }

    public BigDecimal realizedRevenue(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return amount("reporting.realized-revenue", """
            SELECT COALESCE(SUM(amount), 0)
            FROM (
                SELECT br.unit_price * GREATEST(0, LEAST(br.check_out, ?::date) - GREATEST(br.check_in, ?::date)) AS amount
                FROM booking_room br
                WHERE br.is_active AND br.check_in < ?::date AND br.check_out > ?::date
                UNION ALL
                SELECT bs.amount FROM booking_space bs
                WHERE bs.is_active AND bs.event_date >= ?::date AND bs.event_date < ?::date
                UNION ALL
                SELECT bm.amount FROM booking_meeting_room bm
                WHERE bm.is_active AND bm.service_date >= ?::date AND bm.service_date < ?::date
                UNION ALL
                SELECT bp.amount FROM booking_pool bp
                WHERE bp.is_active
                  AND (bp.starts_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
                  AND (bp.starts_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
                UNION ALL
                SELECT bc.amount FROM booking_charge bc
                WHERE bc.voided_at IS NULL AND NOT bc.is_deposit
                  AND (bc.charged_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
                  AND (bc.charged_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
                UNION ALL
                SELECT bo.amount FROM booking_other bo
                WHERE bo.voided_at IS NULL
                  AND (bo.created_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
                  AND (bo.created_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
            ) revenue
            """, toExclusive, from, toExclusive, from, from, toExclusive, from, toExclusive,
            from, toExclusive, from, toExclusive, from, toExclusive);
    }

    public List<Activity> realizedRevenueByActivity(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return measure("reporting.realized-revenue-by-activity", () -> jdbc.query("""
            WITH revenue AS (
                SELECT activity, SUM(amount) AS amount
                FROM (
                SELECT 'Chambre' AS activity,
                       br.unit_price * GREATEST(0, LEAST(br.check_out, ?::date) - GREATEST(br.check_in, ?::date)) AS amount
                FROM booking_room br
                WHERE br.is_active AND br.check_in < ?::date AND br.check_out > ?::date
                UNION ALL
                SELECT 'Espace', bs.amount FROM booking_space bs
                WHERE bs.is_active AND bs.event_date >= ?::date AND bs.event_date < ?::date
                UNION ALL
                SELECT 'Salle de réunion', bm.amount FROM booking_meeting_room bm
                WHERE bm.is_active AND bm.service_date >= ?::date AND bm.service_date < ?::date
                UNION ALL
                SELECT 'Piscine', bp.amount FROM booking_pool bp
                WHERE bp.is_active
                  AND (bp.starts_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
                  AND (bp.starts_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
                UNION ALL
                SELECT CASE WHEN bc.menu_item_id IS NOT NULL THEN 'Restaurant'
                            WHEN bc.additional_service_id IS NOT NULL THEN 'Services'
                            ELSE 'Autres' END,
                       bc.amount
                FROM booking_charge bc
                WHERE bc.voided_at IS NULL AND NOT bc.is_deposit
                  AND (bc.charged_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
                  AND (bc.charged_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
                UNION ALL
                SELECT 'Autres', bo.amount FROM booking_other bo
                WHERE bo.voided_at IS NULL
                  AND (bo.created_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
                  AND (bo.created_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
                ) revenue_rows
                GROUP BY activity
            )
            SELECT activity, amount, SUM(amount) OVER () AS total
            FROM revenue
            ORDER BY CASE activity WHEN 'Chambre' THEN 1 WHEN 'Espace' THEN 2
                 WHEN 'Salle de réunion' THEN 3 WHEN 'Piscine' THEN 4
                 WHEN 'Restaurant' THEN 5 WHEN 'Services' THEN 6 ELSE 7 END
            LIMIT 50
            """, (rs, row) -> new Activity(rs.getString("activity"), rs.getBigDecimal("amount"), rs.getBigDecimal("total")),
            toExclusive, from, toExclusive, from, from, toExclusive, from, toExclusive,
            from, toExclusive, from, toExclusive, from, toExclusive));
    }

    public BigDecimal invoiced(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return amount("reporting.invoiced", """
            SELECT COALESCE(SUM(CASE WHEN i.invoice_type = 'CREDIT_NOTE' THEN -i.total ELSE i.total END), 0)
            FROM invoice i
            JOIN invoice_status s ON s.id_status = i.status_id
            WHERE s.code = 'ISSUED' AND i.issue_date >= ?::date AND i.issue_date < ?::date
            """, from, toExclusive);
    }
    public BigDecimal collected(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return amount("reporting.collected", """
            SELECT COALESCE(SUM(CASE WHEN k.code = 'REFUND' THEN -p.amount ELSE p.amount END), 0)
            FROM payment p
            JOIN payment_kind k ON k.id_kind = p.kind_id
            WHERE k.code IN ('PAYMENT', 'ADVANCE', 'REFUND')
              AND (p.paid_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
              AND (p.paid_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
            """, from, toExclusive);
    }

    public BigDecimal outstanding() {
        return amount("reporting.outstanding", """
            SELECT COALESCE(SUM(GREATEST(balance.balance, 0)), 0)
            FROM v_booking_balance balance
            JOIN booking b ON b.id_booking = balance.id_booking
            JOIN booking_status s ON s.id_booking_status = b.status_id
            WHERE s.code IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')
            """);
    }
    public BigDecimal occupancyRate(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return amount("reporting.occupancy-rate", """
            WITH period AS (SELECT ?::date AS from_date, ?::date AS to_date), room_count AS (
                SELECT COUNT(*) AS value FROM room WHERE NOT is_deleted
            ), stayed AS (
                SELECT COALESCE(SUM(GREATEST(0, LEAST(br.check_out, period.to_date)
                    - GREATEST(br.check_in, period.from_date))), 0) AS nights
                FROM booking_room br
                JOIN booking_line_status line_status ON line_status.id_booking_line_status = br.status_id
                CROSS JOIN period
                WHERE br.is_active AND line_status.code IN ('CHECK_IN', 'CHECK_OUT')
                  AND br.check_in < period.to_date AND br.check_out > period.from_date
            )
            SELECT CASE WHEN room_count.value * (period.to_date - period.from_date) = 0 THEN 0
                ELSE ROUND(100.0 * stayed.nights / (room_count.value * (period.to_date - period.from_date)), 1)
                 END
            FROM period CROSS JOIN room_count CROSS JOIN stayed
            """, from, toExclusive);
    }

    public Series occupancySeries(LocalDate from, LocalDate to, String groupBy) {
        validatePeriod(from, to);
        Grouping grouping = grouping(groupBy, from, to);
        return queryRateSeries("reporting.occupancy-series", from, to, grouping, """
                        SELECT buckets.bucket_start, buckets.bucket_end,
                                     COALESCE(SUM(GREATEST(0, LEAST(br.check_out, buckets.bucket_end)
                     - GREATEST(br.check_in, buckets.bucket_start)))
                                         FILTER (WHERE line_status.code IN ('CHECK_IN', 'CHECK_OUT')), 0) AS numerator,
                   (room_count.value * (buckets.bucket_end - buckets.bucket_start))::numeric AS denominator
            FROM buckets CROSS JOIN room_count
            LEFT JOIN booking_room br ON br.is_active
                AND br.check_in < buckets.bucket_end AND br.check_out > buckets.bucket_start
            LEFT JOIN booking_line_status line_status ON line_status.id_booking_line_status = br.status_id
            GROUP BY buckets.bucket_start, buckets.bucket_end, room_count.value
            """);
    }

    public OccupancyReport occupancy(LocalDate from, LocalDate to, String groupBy, boolean compare) {
        validatePeriod(from, to);
        LocalDate previousFrom = previousFrom(from, to);
        LocalDate previousTo = from.minusDays(1);
        BigDecimal previousRate = compare ? occupancyRate(previousFrom, previousTo) : null;
        List<SeriesPoint> points = occupancySeries(from, to, groupBy).points();
        if (compare) {
            points = mergeSeries(points, occupancySeries(previousFrom, previousTo, groupBy).points());
        }
        return new OccupancyReport(new Comparison(occupancyRate(from, to), previousRate), new Series(points));
    }

    public BigDecimal cancellationRate(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return amount("reporting.cancellation-rate", """
            SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE s.code = 'CANCELED') / NULLIF(COUNT(*), 0), 1), 0)
            FROM booking b
            JOIN booking_status s ON s.id_booking_status = b.status_id
            WHERE (b.created_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
              AND (b.created_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
            """, from, toExclusive);
    }

    public Series cancellationSeries(LocalDate from, LocalDate to, String groupBy) {
        validatePeriod(from, to);
        Grouping grouping = grouping(groupBy, from, to);
        return queryRateSeries("reporting.cancellation-series", from, to, grouping, """
            SELECT buckets.bucket_start, buckets.bucket_end,
                   COUNT(b.id_booking) FILTER (WHERE status.code = 'CANCELED') AS numerator,
                   COUNT(b.id_booking) AS denominator
            FROM buckets
            LEFT JOIN booking b
                ON (b.created_at AT TIME ZONE 'Indian/Antananarivo') >= buckets.bucket_start
               AND (b.created_at AT TIME ZONE 'Indian/Antananarivo') < buckets.bucket_end
            LEFT JOIN booking_status status ON status.id_booking_status = b.status_id
            GROUP BY buckets.bucket_start, buckets.bucket_end
            """);
    }

    public CancellationReport cancellation(LocalDate from, LocalDate to, String groupBy, boolean compare) {
        validatePeriod(from, to);
        LocalDate previousFrom = previousFrom(from, to);
        LocalDate previousTo = from.minusDays(1);
        BigDecimal previousRate = compare ? cancellationRate(previousFrom, previousTo) : null;
        List<SeriesPoint> points = cancellationSeries(from, to, groupBy).points();
        if (compare) {
            points = mergeSeries(points, cancellationSeries(previousFrom, previousTo, groupBy).points());
        }
        return new CancellationReport(new Comparison(cancellationRate(from, to), previousRate), new Series(points));
    }

    public Series bookingsCountSeries(LocalDate from, LocalDate to, String groupBy, boolean compare) {
        validatePeriod(from, to);
        Grouping grouping = grouping(groupBy, from, to);
        Series current = queryBookingCountSeries(from, to, grouping);
        if (!compare) return current;
        LocalDate previousFrom = previousFrom(from, to);
        List<SeriesPoint> points = mergeSeries(current.points(),
                queryBookingCountSeries(previousFrom, from.minusDays(1), grouping).points());
        return new Series(points);
    }

    public BigDecimal averageStay(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return amount("reporting.average-stay", """
            SELECT COALESCE(AVG(br.nights), 0)
            FROM booking_room br
            WHERE br.is_active AND br.check_in >= ?::date AND br.check_in < ?::date
            """, from, toExclusive);
    }

    public BigDecimal averageStayLength(LocalDate from, LocalDate to) {
        return averageStay(from, to);
    }

    public Series averageStaySeries(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return queryAverageStaySeries(from, to);
    }

    public AverageStayReport averageStayReport(LocalDate from, LocalDate to, boolean compare) {
        validatePeriod(from, to);
        LocalDate previousFrom = previousFrom(from, to);
        LocalDate previousTo = from.minusDays(1);
        BigDecimal previous = compare ? averageStayLength(previousFrom, previousTo) : null;
        List<SeriesPoint> points = averageStaySeries(from, to).points();
        if (compare) {
            points = mergeSeries(points, averageStaySeries(previousFrom, previousTo).points());
        }
        return new AverageStayReport(new Comparison(averageStayLength(from, to), previous), new Series(points));
    }

    public AverageStayReport averageStayReport(LocalDate from, LocalDate to, String groupBy, boolean compare) {
        validatePeriod(from, to);
        if (groupBy != null && !groupBy.isBlank()) grouping(groupBy, from, to);
        return averageStayReport(from, to, compare);
    }

    public AverageStayReport averageStay(LocalDate from, LocalDate to, String groupBy, boolean compare) {
        return averageStayReport(from, to, groupBy, compare);
    }

    public List<RoomTypeRate> bookingRateByRoomType(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        LocalDate toExclusive = to.plusDays(1);
        return measure("reporting.booking-rate-by-room-type", () -> jdbc.query("""
              SELECT rt.name,
                    CASE WHEN COUNT(DISTINCT r.id_room) * (params.to_date - params.from_date) = 0 THEN 0
                        ELSE ROUND(COALESCE(SUM(GREATEST(0, LEAST(br.check_out, params.to_date)
                            - GREATEST(br.check_in, params.from_date)))
                            FILTER (WHERE line_status.code IN ('CONFIRMED', 'CHECK_IN', 'CHECK_OUT')), 0) * 100.0
                            / (COUNT(DISTINCT r.id_room) * (params.to_date - params.from_date)), 1)
                    END AS rate,
                    COALESCE(SUM(GREATEST(0, LEAST(br.check_out, params.to_date)
                        - GREATEST(br.check_in, params.from_date)))
                        FILTER (WHERE line_status.code IN ('CONFIRMED', 'CHECK_IN', 'CHECK_OUT')), 0)::bigint AS nights_reserved,
                    (COUNT(DISTINCT r.id_room) * (params.to_date - params.from_date))::bigint AS nights_available
            FROM room_type rt
            CROSS JOIN (SELECT ?::date AS from_date, ?::date AS to_date) params
            LEFT JOIN room r ON r.type_id = rt.id_type AND NOT r.is_deleted
            LEFT JOIN booking_room br ON br.room_id = r.id_room AND br.is_active
                AND br.check_in < params.to_date AND br.check_out > params.from_date
            LEFT JOIN booking_line_status line_status ON line_status.id_booking_line_status = br.status_id
            GROUP BY rt.id_type, rt.name, params.from_date, params.to_date
            ORDER BY rt.name
            LIMIT 50
            """, (rs, row) -> new RoomTypeRate(rs.getString("name"), rs.getBigDecimal("rate"),
                rs.getLong("nights_reserved"), rs.getLong("nights_available")), from, toExclusive));
    }

    public List<RoomTypeRate> bookingRateByRoomType(LocalDate from, LocalDate to, boolean compare) {
        validatePeriod(from, to);
        List<RoomTypeRate> current = bookingRateByRoomType(from, to);
        if (!compare) return current;
        Map<String, BigDecimal> previous = new LinkedHashMap<>();
        bookingRateByRoomType(previousFrom(from, to), from.minusDays(1))
                .forEach(rate -> previous.put(rate.roomType(), rate.rate()));
        return current.stream().map(rate -> new RoomTypeRate(rate.roomType(), rate.rate(),
                previous.getOrDefault(rate.roomType(), BigDecimal.ZERO), rate.nightsReserved(), rate.nightsAvailable())).toList();
    }

    public List<RoomTypeRate> bookingRateByRoomType(LocalDate from, LocalDate to, String groupBy, boolean compare) {
        validatePeriod(from, to);
        if (groupBy != null && !groupBy.isBlank()) grouping(groupBy, from, to);
        return bookingRateByRoomType(from, to, compare);
    }

    public ActivityReport revenueByActivity(LocalDate from, LocalDate to, String groupBy, boolean compare) {
        validatePeriod(from, to);
        if (groupBy != null && !groupBy.isBlank()) grouping(groupBy, from, to);
        return revenueByActivity(from, to, compare);
    }

    public BookingsReport bookingsReport(LocalDate from, LocalDate to, String status, String origin,
            String service, Long customerId, int page, int size) {
        return bookingsReport(from, to, status, origin, service, customerId, page, size, null, false);
    }

    public BookingsReport bookingsReport(LocalDate from, LocalDate to, String status, String origin,
            String service, Long customerId, int page, int size, String sort) {
        return bookingsReport(from, to, status, origin, service, customerId, page, size, sort, false);
    }

    private BookingsReport bookingsReport(LocalDate from, LocalDate to, String status, String origin,
            String service, Long customerId, int page, int size, String sort, boolean export) {
        validatePeriod(from, to);
        if (page < 0 || size < 1 || size > (export ? 10_000 : 50)) {
                throw new IllegalArgumentException("page doit être positif et size compris entre 1 et 50.");
        }
        String normalizedStatus = blankToNull(status);
        String normalizedOrigin = blankToNull(origin);
        String normalizedService = blankToNull(service);
        String orderBy = bookingOrder(sort);
        String sql = """
            WITH service_lines(booking_id, service, service_code) AS (
                SELECT line.booking_id,
                       CASE line.line_type WHEN 'ROOM' THEN 'Chambre' WHEN 'SPACE' THEN 'Espace'
                           WHEN 'MEETING_ROOM' THEN 'Salle de réunion' WHEN 'POOL' THEN 'Piscine' END,
                       line.line_type
                FROM v_booking_line line WHERE line.is_active
                UNION ALL
                SELECT charge.booking_id,
                       CASE WHEN charge.menu_item_id IS NOT NULL THEN 'Restaurant'
                            WHEN charge.additional_service_id IS NOT NULL THEN 'Services' ELSE 'Autres' END,
                       CASE WHEN charge.menu_item_id IS NOT NULL THEN 'RESTAURANT'
                            WHEN charge.additional_service_id IS NOT NULL THEN 'SERVICES' ELSE 'OTHER' END
                FROM booking_charge charge WHERE charge.voided_at IS NULL AND NOT charge.is_deposit
                UNION ALL
                SELECT other.booking_id, 'Autres', 'OTHER'
                FROM booking_other other WHERE other.voided_at IS NULL
            ), service_summary AS (
                SELECT booking_id, string_agg(DISTINCT service, ', ' ORDER BY service) AS services
                FROM service_lines GROUP BY booking_id
            ), filtered AS (
                SELECT b.id_booking, b.ref,
                       CONCAT_WS(' ', NULLIF(c.firstname, ''), NULLIF(c.lastname, '')) AS customer,
                       b.origin, status.code AS status, b.created_at,
                       COALESCE(balance.total, 0) AS total,
                       COALESCE(balance.paid, 0) AS paid,
                       COALESCE(balance.balance, 0) AS balance,
                       services.services,
                       dates.period_start, dates.period_end
                FROM booking b
                JOIN customer c ON c.id_customer = b.customer_id
                JOIN booking_status status ON status.id_booking_status = b.status_id
                LEFT JOIN v_booking_balance balance ON balance.id_booking = b.id_booking
                LEFT JOIN service_summary services ON services.booking_id = b.id_booking
                LEFT JOIN LATERAL (
                    SELECT MIN((line.starts_at AT TIME ZONE 'Indian/Antananarivo')::date) AS period_start,
                           MAX((COALESCE(line.ends_at, line.starts_at) AT TIME ZONE 'Indian/Antananarivo')::date) AS period_end
                    FROM v_booking_line line WHERE line.booking_id = b.id_booking AND line.is_active
                ) dates ON TRUE
                WHERE (b.created_at AT TIME ZONE 'Indian/Antananarivo') >= ?::date
                  AND (b.created_at AT TIME ZONE 'Indian/Antananarivo') < ?::date
                  AND (?::text IS NULL OR status.code = ?)
                  AND (?::text IS NULL OR b.origin = ?)
                  AND (?::integer IS NULL OR b.customer_id = ?)
                  AND (?::text IS NULL OR EXISTS (
                      SELECT 1 FROM service_lines match_service
                      WHERE match_service.booking_id = b.id_booking
                        AND (UPPER(match_service.service) = UPPER(?) OR UPPER(match_service.service_code) = UPPER(?))
                  ))
            ), totals AS (
                SELECT COUNT(*)::bigint AS count, COALESCE(SUM(total), 0) AS total,
                       COALESCE(SUM(paid), 0) AS paid, COALESCE(SUM(balance), 0) AS balance
                FROM filtered
            ), page_rows AS (
                SELECT sorted.* FROM (
                    SELECT filtered.*, ROW_NUMBER() OVER (ORDER BY %s) AS page_order FROM filtered
                ) sorted ORDER BY page_order OFFSET ? LIMIT ?
            )
            SELECT page_rows.*, totals.count AS total_count, totals.total AS total_amount,
                   totals.paid AS total_paid, totals.balance AS total_balance
            FROM totals LEFT JOIN page_rows ON TRUE
            ORDER BY page_rows.page_order NULLS LAST
            """.formatted(orderBy);
        Object[] args = { from, to.plusDays(1), normalizedStatus, normalizedStatus, normalizedOrigin,
                normalizedOrigin, customerId, customerId, normalizedService, normalizedService, normalizedService,
                (long) page * size, size };
        ResultSetExtractor<BookingsReport> extractor = rs -> {
            List<BookingReportRow> items = new ArrayList<>();
            BookingTotals totals = null;
            while (rs.next()) {
                totals = new BookingTotals(rs.getLong("total_count"), rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("total_paid"), rs.getBigDecimal("total_balance"));
                if (rs.getObject("id_booking") != null) {
                    items.add(new BookingReportRow(rs.getLong("id_booking"), rs.getString("ref"),
                            rs.getString("customer"), rs.getString("origin"), rs.getString("status"),
                            rs.getObject("period_start", LocalDate.class), rs.getObject("period_end", LocalDate.class),
                            rs.getString("services"), rs.getBigDecimal("total"), rs.getBigDecimal("paid"),
                            rs.getBigDecimal("balance"), rs.getObject("created_at", OffsetDateTime.class)));
                }
            }
            return new BookingsReport(items, totals, page, size);
        };
        return measure("reporting.bookings", () -> jdbc.query(sql, extractor, args));
    }

    /** Uses the displayed report query with its export limit; it never introduces a second SQL shape. */
    public BookingsReport bookingsExport(LocalDate from, LocalDate to, String status, String origin,
            String service, Long customerId) {
        BookingsReport report = bookingsReport(from, to, status, origin, service, customerId, 0, 10_000, null, true);
        if (report.totals().count() > 10_000) throw new IllegalArgumentException("Réduisez la période");
        return report;
    }

    private Series queryRateSeries(String queryName, LocalDate from, LocalDate to, Grouping grouping, String metricSql) {
        String sql = """
            WITH period AS (SELECT ?::date AS from_date, ?::date + 1 AS to_date), bucket_starts AS (
                SELECT generate_series(
                    date_trunc('%s', from_date::timestamp),
                    date_trunc('%s', (to_date - 1)::timestamp), INTERVAL '%s'
                )::date AS label_date
                FROM period
            ), buckets AS (
                SELECT label_date, GREATEST(label_date, period.from_date) AS bucket_start,
                       LEAST((label_date + INTERVAL '%s')::date, period.to_date) AS bucket_end,
                       to_char(label_date, '%s') AS label
                FROM bucket_starts CROSS JOIN period
            ), room_count AS (
                SELECT COUNT(*) AS value FROM room WHERE NOT is_deleted
            ), metric AS (
                %s
            )
            SELECT buckets.label,
                   CASE WHEN COALESCE(metric.denominator, 0) = 0 THEN 0
                        ELSE ROUND(100.0 * COALESCE(metric.numerator, 0) / metric.denominator, 1)
                   END AS value
            FROM buckets JOIN metric USING (bucket_start, bucket_end)
            ORDER BY buckets.label_date
            """.formatted(grouping.trunc(), grouping.trunc(), grouping.interval(), grouping.interval(),
                grouping.format(), metricSql);
        List<SeriesPoint> points = measure(queryName, () -> jdbc.query(sql,
                (rs, row) -> new SeriesPoint(rs.getString("label"), zero(rs.getBigDecimal("value")), null), from, to));
        return new Series(points);
    }

    private Series queryAverageStaySeries(LocalDate from, LocalDate to) {
        LocalDate toExclusive = to.plusDays(1);
        List<SeriesPoint> points = measure("reporting.average-stay-series", () -> jdbc.query("""
            WITH period AS (SELECT ?::date AS from_date, ?::date AS to_date), months AS (
                SELECT generate_series(date_trunc('month', from_date::timestamp),
                       date_trunc('month', (to_date - 1)::timestamp), INTERVAL '1 month')::date AS month_start
                FROM period
            )
            SELECT to_char(months.month_start, 'YYYY-MM') AS label,
                   COALESCE(AVG(br.nights), 0) AS value
            FROM months CROSS JOIN period
            LEFT JOIN booking_room br ON br.is_active
                AND br.check_in >= GREATEST(months.month_start, period.from_date)
                AND br.check_in < LEAST((months.month_start + INTERVAL '1 month')::date, period.to_date)
            GROUP BY months.month_start ORDER BY months.month_start
            """, (rs, row) -> new SeriesPoint(rs.getString("label"), zero(rs.getBigDecimal("value")), null),
                from, toExclusive));
        return new Series(points);
    }

    private Series queryBookingCountSeries(LocalDate from, LocalDate to, Grouping grouping) {
        List<SeriesPoint> points = measure("reporting.bookings-count-series", () -> jdbc.query("""
            WITH period AS (SELECT ?::date AS from_date, ?::date + 1 AS to_date), bucket_starts AS (
                SELECT generate_series(
                    date_trunc('%s', from_date::timestamp),
                    date_trunc('%s', (to_date - 1)::timestamp), INTERVAL '%s'
                )::date AS label_date
                FROM period
            ), buckets AS (
                SELECT label_date, GREATEST(label_date, period.from_date) AS bucket_start,
                       LEAST((label_date + INTERVAL '%s')::date, period.to_date) AS bucket_end,
                       to_char(label_date, '%s') AS label
                FROM bucket_starts CROSS JOIN period
            )
            SELECT buckets.label, COUNT(booking.id_booking)::numeric AS value
            FROM buckets LEFT JOIN booking
                ON (booking.created_at AT TIME ZONE 'Indian/Antananarivo') >= buckets.bucket_start
               AND (booking.created_at AT TIME ZONE 'Indian/Antananarivo') < buckets.bucket_end
            GROUP BY buckets.label_date, buckets.label ORDER BY buckets.label_date
            """.formatted(grouping.trunc(), grouping.trunc(), grouping.interval(), grouping.interval(),
                grouping.format()),
                (rs, row) -> new SeriesPoint(rs.getString("label"), zero(rs.getBigDecimal("value")), null), from, to));
        return new Series(points);
    }

    private Map<String, BigDecimal> activityAmounts(List<Activity> activities) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        activities.forEach(activity -> result.put(activity.label(), zero(activity.amount())));
        return result;
    }

    private List<SeriesPoint> mergeSeries(List<SeriesPoint> current, List<SeriesPoint> previous) {
        List<SeriesPoint> merged = new ArrayList<>(current.size());
        for (int index = 0; index < current.size(); index++) {
            SeriesPoint point = current.get(index);
            BigDecimal previousValue = index < previous.size() ? previous.get(index).value() : BigDecimal.ZERO;
            merged.add(new SeriesPoint(point.label(), point.value(), previousValue));
        }
        return merged;
    }

    private Grouping grouping(String groupBy, LocalDate from, LocalDate to) {
        String value = groupBy == null || groupBy.isBlank()
                ? (ChronoUnit.DAYS.between(from, to) + 1 > 62 ? "month" : "day")
                : groupBy.toLowerCase(java.util.Locale.ROOT);
        return switch (value) {
            case "day" -> new Grouping("day", "1 day", "1 day", "YYYY-MM-DD");
            case "week" -> new Grouping("week", "1 week", "1 week", "YYYY-MM-DD");
            case "month" -> new Grouping("month", "1 month", "1 month", "YYYY-MM");
            default -> throw new IllegalArgumentException("groupBy doit être day, week ou month.");
        };
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("from et to sont obligatoires.");
        if (from.isAfter(to)) throw new IllegalArgumentException("from doit être antérieur ou égal à to.");
        if (to.isAfter(from.plusYears(5))) {
            throw new IllegalArgumentException("La période ne peut pas dépasser 5 ans.");
        }
    }

    private LocalDate previousFrom(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return from.minusDays(days);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String bookingOrder(String requestedSort) {
        if (requestedSort == null || requestedSort.isBlank()) return "created_at DESC, id_booking DESC";
        String[] parts = requestedSort.split(",", -1);
        String column = switch (parts[0].trim()) {
            case "reference" -> "ref";
            case "customer" -> "customer";
            case "origin" -> "origin";
            case "status" -> "status";
            case "periodStart" -> "period_start";
            case "periodEnd" -> "period_end";
            case "total" -> "total";
            case "paid" -> "paid";
            case "balance" -> "balance";
            case "createdAt" -> "created_at";
            default -> throw new IllegalArgumentException("Tri de réservation invalide.");
        };
        String direction = parts.length > 1 ? parts[1].trim().toUpperCase(java.util.Locale.ROOT) : "ASC";
        if (!direction.equals("ASC") && !direction.equals("DESC")) {
            throw new IllegalArgumentException("Sens de tri invalide.");
        }
        return column + " " + direction + ", id_booking DESC";
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String monthLabel(RevenueMonthRow row) {
        return "%04d-%02d".formatted(row.year(), row.month());
    }

    private String monthKey(int year, int month) {
        return "%04d-%02d".formatted(year, month);
    }

    private List<SeriesPoint> monthSeries(List<Integer> years, Map<String, RevenueMonthRow> rows,
            boolean compare, Function<RevenueMonthRow, BigDecimal> amount) {
        List<SeriesPoint> points = new ArrayList<>();
        for (int year : years) {
            for (int month = 1; month <= 12; month++) {
                RevenueMonthRow current = rows.get(monthKey(year, month));
                RevenueMonthRow previous = rows.get(monthKey(year - 1, month));
                points.add(new SeriesPoint(monthKey(year, month), current == null ? BigDecimal.ZERO : amount.apply(current),
                        compare ? previous == null ? BigDecimal.ZERO : amount.apply(previous) : null));
            }
        }
        return points;
    }

    private record Grouping(String trunc, String interval, String step, String format) {}
    private record RevenueMonthRow(int year, int month, BigDecimal realized, BigDecimal invoiced, BigDecimal collected) {}

    private <T> T measure(String queryName, Supplier<T> query) {
        long startedAt = System.nanoTime();
        try {
            return query.get();
        } finally {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            if (elapsedMillis > SLOW_QUERY_MILLIS) {
                log.warn("Slow reporting query {} took {} ms", queryName, elapsedMillis);
            }
        }
    }
}
