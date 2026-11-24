package mg.lagrace.api.billing.persistence;

import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.payment.Payment;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import mg.lagrace.api.billing.dto.invoice.InvoiceArchiveResponse;

@Repository
public class InvoiceArchiveRepository {
        private static final String FROM = """
            FROM invoice i
            JOIN v_invoice_summary s ON s.id_invoice = i.id_invoice
            """;
    @PersistenceContext
    private EntityManager entityManager;

    public List<InvoiceArchiveResponse.Item> findItems(String search, String paymentStatus,
            String service, String type, LocalDate from, LocalDate to,
            int offset, int limit, String sort) {
        String sql = """
                SELECT i.id_invoice, i.invoice_number, i.issue_date, i.billing_name, i.reference_label,
                       CASE WHEN services.service_count = 0 THEN '—'
                            WHEN services.service_count > 1 THEN 'Plusieurs services'
                            ELSE services.service_label END AS service_label,
                       i.total, s.net_total, COALESCE(payment_method.label, '—') AS payment_method_label,
                       s.payment_status, s.status_code, i.invoice_type,
                       COALESCE(credits.invoice_numbers, '') AS credited_by_numbers,
                       parent.invoice_number AS credited_invoice_number, i.booking_id
                """ + FROM + archiveJoins() + where(search, paymentStatus, service, type, from, to) + " ORDER BY " + orderBy(sort)
                + " LIMIT :limit OFFSET :offset";

        Query query = entityManager.createNativeQuery(sql);
        bind(query, search, paymentStatus, service, type, from, to);
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::item).toList();
    }

    public long count(String search, String paymentStatus, String service, String type,
            LocalDate from, LocalDate to) {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) " + FROM + where(search, paymentStatus, service, type, from, to));
        bind(query, search, paymentStatus, service, type, from, to);
        return ((Number) query.getSingleResult()).longValue();
    }

    public InvoiceArchiveResponse.Totals totals(String search, String paymentStatus, String service,
            String type, LocalDate from, LocalDate to) {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE i.invoice_type = 'INVOICE'),
                       COUNT(*) FILTER (WHERE i.invoice_type = 'INVOICE' AND s.payment_status = 'PAID'),
                       COALESCE(SUM(s.net_total - s.paid_on_invoice) FILTER (
                           WHERE i.invoice_type = 'INVOICE' AND s.payment_status IN ('UNPAID', 'PARTIAL')), 0),
                       COALESCE(SUM(s.net_total) FILTER (WHERE i.invoice_type = 'INVOICE'), 0)
                """ + FROM + where(search, paymentStatus, service, type, from, to);
        Query query = entityManager.createNativeQuery(sql);
        bind(query, search, paymentStatus, service, type, from, to);
        Object[] values = (Object[]) query.getSingleResult();
        return new InvoiceArchiveResponse.Totals(((Number) values[0]).longValue(),
                ((Number) values[1]).longValue(), decimal(values[2]), decimal(values[3]));
    }

    public List<InvoiceArchiveResponse.Item> exportItems(String search, String paymentStatus,
            String service, String type, LocalDate from, LocalDate to, String sort) {
        String sql = """
                SELECT i.id_invoice, i.invoice_number, i.issue_date, i.billing_name, i.reference_label,
                       CASE WHEN services.service_count = 0 THEN '—'
                            WHEN services.service_count > 1 THEN 'Plusieurs services'
                            ELSE services.service_label END AS service_label,
                       i.total, s.net_total, COALESCE(payment_method.label, '—') AS payment_method_label,
                       s.payment_status, s.status_code, i.invoice_type,
                       COALESCE(credits.invoice_numbers, '') AS credited_by_numbers,
                       parent.invoice_number AS credited_invoice_number, i.booking_id
                """ + FROM + archiveJoins() + where(search, paymentStatus, service, type, from, to) + " ORDER BY " + orderBy(sort);
        Query query = entityManager.createNativeQuery(sql);
        bind(query, search, paymentStatus, service, type, from, to);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::item).toList();
    }

    private String archiveJoins() {
        return """
                LEFT JOIN LATERAL (
                    SELECT COUNT(DISTINCT line.service_code) AS service_count,
                           CASE MIN(line.service_code)
                             WHEN 'ROOM' THEN 'Chambre' WHEN 'SPACE' THEN 'Espace'
                             WHEN 'MEETING_ROOM' THEN 'Salle de réunion' WHEN 'POOL' THEN 'Piscine'
                             WHEN 'RESTAURANT' THEN 'Restaurant' WHEN 'EXTRA' THEN 'Service supplémentaire'
                             WHEN 'OTHER' THEN 'Autre' ELSE '—' END AS service_label
                    FROM invoice_line line WHERE line.invoice_id = i.id_invoice
                ) services ON TRUE
                LEFT JOIN LATERAL (
                    SELECT CASE WHEN COUNT(DISTINCT method.name) > 1 THEN 'Mixte'
                                ELSE (ARRAY_AGG(method.name ORDER BY p.paid_at DESC, p.id_payment DESC))[1]
                           END AS label
                    FROM payment p JOIN payment_method method ON method.id_method = p.method_id
                    JOIN payment_kind kind ON kind.id_kind = p.kind_id
                    WHERE p.booking_id = i.booking_id AND kind.code IN ('PAYMENT', 'ADVANCE', 'REFUND')
                ) payment_method ON TRUE
                LEFT JOIN LATERAL (
                    SELECT STRING_AGG(credit.invoice_number, CHR(31) ORDER BY credit.issue_date, credit.id_invoice) AS invoice_numbers
                    FROM invoice credit WHERE credit.credited_invoice_id = i.id_invoice
                ) credits ON TRUE
                LEFT JOIN invoice parent ON parent.id_invoice = i.credited_invoice_id
                """;
    }

    private String orderBy(String sort) {
        String key = "issueDate";
        String direction = "DESC";
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String requested = parts[0].trim();
            if (List.of("issueDate", "number", "customerName", "total").contains(requested)) key = requested;
            if (parts.length == 2 && "asc".equalsIgnoreCase(parts[1].trim())) direction = "ASC";
        }
        String column = switch (key) {
            case "number" -> "i.invoice_number";
            case "customerName" -> "i.billing_name";
            case "total" -> "i.total";
            default -> "i.issue_date";
        };
        return column + " " + direction + ", i.id_invoice DESC";
    }

    private void bind(Query query, String search, String paymentStatus, String service, String type,
            LocalDate from, LocalDate to) {
        String normalizedSearch = blankToNull(search);
        String normalizedPaymentStatus = blankToNull(paymentStatus);
        String normalizedService = blankToNull(service);
        String normalizedType = blankToNull(type);
        if (normalizedSearch != null) query.setParameter("search", normalizedSearch);
        if (normalizedPaymentStatus != null) query.setParameter("paymentStatus", normalizedPaymentStatus);
        if (normalizedService != null) query.setParameter("service", normalizedService);
        if (normalizedType != null) query.setParameter("type", normalizedType);
        if (from != null) query.setParameter("fromDate", Date.valueOf(from));
        if (to != null) query.setParameter("toDate", Date.valueOf(to));
    }

    private String where(String search, String paymentStatus, String service, String type,
            LocalDate from, LocalDate to) {
        StringBuilder where = new StringBuilder("\n WHERE 1 = 1");
        if (blankToNull(search) != null) {
            where.append("""
                  AND (LOWER(i.invoice_number) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.billing_name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(i.reference_label, '')) LIKE LOWER(CONCAT('%', :search, '%')))
                """);
        }
        if (blankToNull(paymentStatus) != null) {
            where.append("\n AND s.payment_status = :paymentStatus");
        }
        if (blankToNull(service) != null) {
            where.append("""
                  AND EXISTS (
                    SELECT 1 FROM invoice_line filter_line
                    WHERE filter_line.invoice_id = i.id_invoice
                      AND filter_line.service_code = :service)
                """);
        }
        if (blankToNull(type) != null) {
            where.append("\n AND i.invoice_type = :type");
        }
        if (from != null) {
            where.append("\n AND i.issue_date >= :fromDate");
        }
        if (to != null) {
            where.append("\n AND i.issue_date <= :toDate");
        }
        return where.toString();
    }

    private InvoiceArchiveResponse.Item item(Object[] row) {
        String credited = (String) row[12];
        List<String> creditedBy = credited == null || credited.isEmpty()
                ? List.of() : new ArrayList<>(List.of(credited.split(String.valueOf((char) 31))));
        String type = (String) row[11];
        BigDecimal total = decimal(row[6]);
        if ("CREDIT_NOTE".equals(type)) total = total.negate();
        return new InvoiceArchiveResponse.Item(((Number) row[0]).longValue(), ((Number) row[14]).longValue(),
            (String) row[1], localDate(row[2]), (String) row[3], (String) row[4], (String) row[5], total,
            decimal(row[7]), (String) row[8], (String) row[9], (String) row[10], type, creditedBy, (String) row[13]);
    }

    private static BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : (BigDecimal) value; }
    private static LocalDate localDate(Object value) {
        return value instanceof Date date ? date.toLocalDate() : (LocalDate) value;
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}