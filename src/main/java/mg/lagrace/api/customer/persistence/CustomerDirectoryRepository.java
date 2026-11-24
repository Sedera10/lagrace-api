package mg.lagrace.api.customer.persistence;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.reference.Nationality;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import mg.lagrace.api.customer.dto.CustomerListResponse;

@Repository
public class CustomerDirectoryRepository {
    private static final String FROM = """
            FROM customer c
            LEFT JOIN nationality n ON n.id_nationality = c.nationality_id
            LEFT JOIN v_customer_summary s ON s.id_customer = c.id_customer
            """;
    private static final String BASE_FILTERS = """
            WHERE c.id_customer <> :walkInCustomerId
              AND (:activeOnly = FALSE OR c.is_active = TRUE)
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public List<CustomerListResponse.Item> findItems(Long walkInCustomerId, boolean activeOnly,
            Long tagId, Boolean hasBalance, String search, SearchMode searchMode, List<String> words,
            int offset, int limit, String sort) {
        String sql = """
                SELECT c.id_customer, CONCAT_WS(' ', c.firstname, c.lastname) AS full_name, c.phone,
                       n.name AS nationality, COALESCE(s.bookings_count, 0), s.last_visit, s.next_visit,
                       COALESCE(s.total_billed, 0), COALESCE(s.balance_due, 0), COALESCE(s.refund_due, 0), c.is_active
                """ + FROM + filters(tagId, hasBalance) + searchClause(search, searchMode, words)
                + " ORDER BY " + orderBy(sort) + " LIMIT :limit OFFSET :offset";
        Query query = entityManager.createNativeQuery(sql);
        bind(query, walkInCustomerId, activeOnly, tagId, hasBalance, search, searchMode, words);
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<Long> ids = rows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, List<CustomerListResponse.Tag>> tags = findTags(ids);
        return rows.stream().map(row -> new CustomerListResponse.Item(((Number) row[0]).longValue(),
                (String) row[1], (String) row[2], (String) row[3], tags.getOrDefault(((Number) row[0]).longValue(), List.of()),
                ((Number) row[4]).longValue(), localDate(row[5]), localDate(row[6]), decimal(row[7]),
                decimal(row[8]), decimal(row[9]), Boolean.TRUE.equals(row[10]))).toList();
    }

    public long count(Long walkInCustomerId, boolean activeOnly, Long tagId, Boolean hasBalance,
            String search, SearchMode searchMode, List<String> words) {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) " + FROM + filters(tagId, hasBalance)
                + searchClause(search, searchMode, words));
        bind(query, walkInCustomerId, activeOnly, tagId, hasBalance, search, searchMode, words);
        return ((Number) query.getSingleResult()).longValue();
    }

    public CustomerListResponse.Totals totals(Long walkInCustomerId, boolean activeOnly, Long tagId,
            Boolean hasBalance, String search, SearchMode searchMode, List<String> words) {
        String sql = """
                SELECT COUNT(*), COUNT(*) FILTER (WHERE COALESCE(s.balance_due, 0) > 0),
                       COALESCE(SUM(GREATEST(COALESCE(s.balance_due, 0), 0)), 0)
                """ + FROM + filters(tagId, hasBalance) + searchClause(search, searchMode, words);
        Query query = entityManager.createNativeQuery(sql);
        bind(query, walkInCustomerId, activeOnly, tagId, hasBalance, search, searchMode, words);
        Object[] values = (Object[]) query.getSingleResult();
        return new CustomerListResponse.Totals(((Number) values[0]).longValue(),
                ((Number) values[1]).longValue(), decimal(values[2]));
    }

    public List<CustomerListResponse.Tag> findActiveTags() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id_tag, name, color_code FROM customer_tag WHERE is_active ORDER BY name
                """).getResultList();
        return rows.stream().map(row -> new CustomerListResponse.Tag(((Number) row[0]).longValue(),
                (String) row[1], (String) row[2])).toList();
    }

        public java.util.Set<Long> findAlertCustomerIds(List<Long> customerIds) {
                if (customerIds.isEmpty()) return java.util.Set.of();
                String placeholders = java.util.stream.IntStream.range(0, customerIds.size())
                                .mapToObj(index -> ":alertCustomer" + index).collect(java.util.stream.Collectors.joining(", "));
                Query query = entityManager.createNativeQuery("""
                                SELECT DISTINCT customer_id FROM customer_note
                                WHERE is_alert AND archived_at IS NULL AND customer_id IN (
                                """ + placeholders + ")");
                for (int index = 0; index < customerIds.size(); index++)
                        query.setParameter("alertCustomer" + index, customerIds.get(index));
                @SuppressWarnings("unchecked")
                List<Number> rows = query.getResultList();
                return rows.stream().map(Number::longValue).collect(java.util.stream.Collectors.toSet());
        }

    private Map<Long, List<CustomerListResponse.Tag>> findTags(List<Long> customerIds) {
        if (customerIds.isEmpty()) return Map.of();
        String placeholders = java.util.stream.IntStream.range(0, customerIds.size())
                .mapToObj(index -> ":customer" + index).collect(java.util.stream.Collectors.joining(", "));
        Query query = entityManager.createNativeQuery("""
                SELECT link.customer_id, tag.id_tag, tag.name, tag.color_code
                FROM customer_tag_link link JOIN customer_tag tag ON tag.id_tag = link.tag_id
                WHERE link.customer_id IN (
                """ + placeholders + ") ORDER BY tag.name");
        for (int index = 0; index < customerIds.size(); index++)
            query.setParameter("customer" + index, customerIds.get(index));
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Map<Long, List<CustomerListResponse.Tag>> result = new HashMap<>();
        for (Object[] row : rows) {
            Long customerId = ((Number) row[0]).longValue();
            result.computeIfAbsent(customerId, ignored -> new ArrayList<>())
                    .add(new CustomerListResponse.Tag(((Number) row[1]).longValue(), (String) row[2], (String) row[3]));
        }
        return result;
    }

    private String searchClause(String search, SearchMode mode, List<String> words) {
        if (search == null) return "";
        if (mode == SearchMode.PHONE) return " AND c.phone_key = :phoneKey";
        if (mode == SearchMode.EMAIL) return " AND LOWER(UNACCENT(COALESCE(c.email, ''))) LIKE :emailPattern";
        StringBuilder clause = new StringBuilder(" AND (");
        if (mode == SearchMode.TEXT) {
            for (int index = 0; index < words.size(); index++) {
                if (index > 0) clause.append(" AND ");
                clause.append("(UNACCENT(LOWER(COALESCE(c.lastname, ''))) LIKE :word")
                        .append(index).append(" OR UNACCENT(LOWER(COALESCE(c.firstname, ''))) LIKE :word")
                        .append(index).append(')');
            }
            clause.append(" OR c.identification_number = :exactDocument")
                    .append(" OR EXISTS (SELECT 1 FROM booking b WHERE b.customer_id = c.id_customer")
                    .append(" AND UNACCENT(LOWER(b.ref)) LIKE :referencePattern)");
        }
        return clause.append(')').toString();
    }

        private String filters(Long tagId, Boolean hasBalance) {
                StringBuilder filters = new StringBuilder(BASE_FILTERS);
                if (tagId != null) filters.append(" AND EXISTS (SELECT 1 FROM customer_tag_link selected_tag")
                                .append(" WHERE selected_tag.customer_id=c.id_customer AND selected_tag.tag_id=:tagId)");
                if (hasBalance != null) filters.append(" AND (COALESCE(s.balance_due, 0) > 0)=:hasBalance");
                return filters.toString();
        }

    private String orderBy(String sort) {
        String field = sort == null ? "name" : sort.split(",", 2)[0].trim();
        String direction = sort != null && sort.toLowerCase().endsWith(",desc") ? "DESC" : "ASC";
        String expression = switch (field) {
            case "lastVisit" -> "s.last_visit";
            case "totalBilled" -> "s.total_billed";
            case "balanceDue" -> "s.balance_due";
            default -> "LOWER(UNACCENT(c.lastname)), LOWER(UNACCENT(c.firstname))";
        };
        return expression + " " + direction + " NULLS LAST, c.id_customer ASC";
    }

    private void bind(Query query, Long walkInCustomerId, boolean activeOnly, Long tagId, Boolean hasBalance,
            String search, SearchMode mode, List<String> words) {
        query.setParameter("walkInCustomerId", walkInCustomerId);
        query.setParameter("activeOnly", activeOnly);
        if (tagId != null) query.setParameter("tagId", tagId);
        if (hasBalance != null) query.setParameter("hasBalance", hasBalance);
        if (search == null) return;
        if (mode == SearchMode.PHONE) query.setParameter("phoneKey", search.length() > 9 ? search.substring(search.length() - 9) : search);
        else if (mode == SearchMode.EMAIL) query.setParameter("emailPattern", "%" + search + "%");
        else {
            for (int index = 0; index < words.size(); index++) query.setParameter("word" + index, "%" + words.get(index) + "%");
            query.setParameter("exactDocument", search);
            query.setParameter("referencePattern", "%" + search + "%");
        }
    }

    private BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : (BigDecimal) value; }
    private LocalDate localDate(Object value) { return value instanceof Date date ? date.toLocalDate() : (LocalDate) value; }
    public enum SearchMode { PHONE, EMAIL, TEXT }
}