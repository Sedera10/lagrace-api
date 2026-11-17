package mg.lagrace.api.services;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.text.Normalizer;
import java.util.Arrays;
import java.math.BigDecimal;
import java.sql.Array;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import mg.lagrace.api.dto.customer.CustomerBookingsResponse;
import mg.lagrace.api.dto.customer.CustomerFileResponse;
import mg.lagrace.api.dto.customer.CustomerAlertsResponse;
import mg.lagrace.api.dto.customer.CustomerInvoicesResponse;
import mg.lagrace.api.dto.customer.CustomerPaymentsResponse;
import mg.lagrace.api.exceptions.ResourceNotFoundException;

import mg.lagrace.api.dto.customer.CustomerResponse;
import mg.lagrace.api.dto.customer.CustomerListResponse;
import mg.lagrace.api.dto.customer.CustomerSearchResponse;
import mg.lagrace.api.dto.customer.CustomerDuplicateRequest;
import mg.lagrace.api.dto.customer.CustomerDuplicateResponse;
import mg.lagrace.api.dto.customer.CustomerMutationRequests;
import mg.lagrace.api.dto.customer.CustomerNoteResponse;
import mg.lagrace.api.dto.customer.CustomerChangeResponse;
import mg.lagrace.api.dto.customer.CustomerChangesResponse;
import mg.lagrace.api.dto.customer.CustomerTagResponse;
import mg.lagrace.api.repositories.CustomerRepository;
import mg.lagrace.api.repositories.CustomerDirectoryRepository;
import mg.lagrace.api.repositories.CustomerDirectoryRepository.SearchMode;
import mg.lagrace.api.repositories.UserRepository;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.BusinessException;

@Service
public class CustomerService {
    private static final int MAX_AUTOCOMPLETE_RESULTS = 50;
    private static final int MIN_SEARCH_LENGTH = 2;

    private final CustomerRepository customerRepository;
    @Value("${customer.walk-in-customer-id:1}")
    private Long walkInCustomerId;
    private final JdbcTemplate jdbc;
    private final CustomerDirectoryRepository directory;
    private final UserRepository userRepository;
    @Value("${customer.level.regular-min-completed:3}")
    private long regularMinCompleted;
    @Value("${customer.level.vip-min-total-paid:}")
    private String vipMinTotalPaid;
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    public CustomerService(CustomerRepository customerRepository, JdbcTemplate jdbc,
            CustomerDirectoryRepository directory, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.jdbc = jdbc;
        this.directory = directory;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerSearchResponse> search(String query, int limit) {
        if (query == null || query.isBlank() || query.trim().length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        int pageSize = Math.max(1, Math.min(limit, MAX_AUTOCOMPLETE_RESULTS));
        String term = query.trim();
        SearchMode mode = term.matches("\\d+") ? SearchMode.PHONE : term.contains("@") ? SearchMode.EMAIL : SearchMode.TEXT;
        List<String> words = mode == SearchMode.TEXT
                ? Arrays.stream(normalize(term).split("\\s+")).filter(word -> !word.isBlank()).toList() : List.of();
        List<CustomerListResponse.Item> items = directory.findItems(walkInCustomerId, true, null, null,
                term, mode, words, 0, pageSize, "name,asc");
        java.util.Set<Long> alertCustomerIds = directory.findAlertCustomerIds(items.stream().map(CustomerListResponse.Item::id).toList());
        return items.stream().map(item -> new CustomerSearchResponse(item.id(), item.fullName(), item.phone(), item.tags(),
                item.balanceDue(), item.nextVisit(), alertCustomerIds.contains(item.id()))).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDuplicateResponse checkDuplicates(CustomerDuplicateRequest request) {
        String lastname = request.lastname().trim();
        String firstname = request.firstname().trim();
        String phoneKey = phoneKey(request.phone());
        boolean hasDocument = request.documentId() != null && request.identificationNumber() != null
                && !request.identificationNumber().isBlank();
        List<CustomerDuplicateResponse.Candidate> candidates = jdbc.query("""
            select c.id_customer, concat_ws(' ', c.firstname, c.lastname), c.phone,
                   coalesce(s.bookings_count, 0), s.last_visit, coalesce(s.balance_due, 0),
                   (cast(? as integer) is not null and c.document_id=? and c.identification_number=?) as same_document,
                   (cast(? as varchar) is not null and c.phone_key=?) as same_phone,
                   (cast(? as date) is not null and c.date_of_birth=cast(? as date)
                     and unaccent(lower(c.lastname))=unaccent(lower(?))
                     and unaccent(lower(c.firstname))=unaccent(lower(?))) as same_name_birth
              from customer c left join v_customer_summary s on s.id_customer=c.id_customer
             where c.is_active and c.merged_into_id is null and c.id_customer<>?
               and ((cast(? as integer) is not null and c.document_id=? and c.identification_number=?)
                 or (cast(? as varchar) is not null and c.phone_key=?)
                 or (cast(? as date) is not null and c.date_of_birth=cast(? as date)
                     and unaccent(lower(c.lastname))=unaccent(lower(?))
                     and unaccent(lower(c.firstname))=unaccent(lower(?))))
             order by same_document desc, same_phone desc, same_name_birth desc, c.id_customer limit 5
            """, (rs, row) -> {
                List<String> reasons = new ArrayList<>();
                if (rs.getBoolean(7)) reasons.add("SAME_DOCUMENT");
                if (rs.getBoolean(8)) reasons.add("SAME_PHONE");
                if (rs.getBoolean(9)) reasons.add("SAME_NAME_AND_BIRTHDATE");
                return new CustomerDuplicateResponse.Candidate(rs.getLong(1), rs.getString(2), rs.getString(3),
                    rs.getLong(4), rs.getObject(5, LocalDate.class), money(rs.getBigDecimal(6)), reasons);
            },
            hasDocument ? request.documentId() : null, hasDocument ? request.documentId() : null,
            hasDocument ? request.identificationNumber() : null,
            phoneKey, phoneKey, request.dateOfBirth(), request.dateOfBirth(), lastname, firstname, walkInCustomerId,
            hasDocument ? request.documentId() : null, hasDocument ? request.documentId() : null,
            hasDocument ? request.identificationNumber() : null,
            phoneKey, phoneKey, request.dateOfBirth(), request.dateOfBirth(), lastname, firstname);

        if (candidates.isEmpty()) {
            candidates = jdbc.query("""
                select c.id_customer, concat_ws(' ', c.firstname, c.lastname), c.phone,
                       coalesce(s.bookings_count, 0), s.last_visit, coalesce(s.balance_due, 0)
                  from customer c left join v_customer_summary s on s.id_customer=c.id_customer
                 where c.is_active and c.merged_into_id is null and c.id_customer<>?
                   and unaccent(lower(c.lastname))=unaccent(lower(?))
                   and left(unaccent(lower(c.firstname)), 1)=left(unaccent(lower(?)), 1)
                 order by c.lastname, c.firstname, c.id_customer limit 5
                """, (rs, row) -> new CustomerDuplicateResponse.Candidate(rs.getLong(1), rs.getString(2),
                    rs.getString(3), rs.getLong(4), rs.getObject(5, LocalDate.class), money(rs.getBigDecimal(6)),
                    List.of("SIMILAR_NAME")), walkInCustomerId, lastname, firstname);
        }
        List<CustomerDuplicateResponse.Candidate> ordered = candidates.stream()
            .sorted(java.util.Comparator.comparingInt(this::duplicateStrength).thenComparing(CustomerDuplicateResponse.Candidate::id))
            .limit(5).toList();
        return new CustomerDuplicateResponse(ordered);
    }

    private int duplicateStrength(CustomerDuplicateResponse.Candidate candidate) {
        if (candidate.reasons().contains("SAME_DOCUMENT")) return 0;
        if (candidate.reasons().contains("SAME_PHONE")) return 1;
        if (candidate.reasons().contains("SAME_NAME_AND_BIRTHDATE")) return 2;
        return 3;
    }

    private static String phoneKey(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        return digits.length() < 8 ? null : digits.substring(Math.max(0, digits.length() - 9));
    }

            @Transactional(readOnly = true)
            public CustomerListResponse list(String search, Long tagId, Boolean hasBalance, Boolean active,
                int page, int size, String sort) {
            if (search != null && !search.isBlank() && search.trim().length() < 2)
                throw new mg.lagrace.api.exceptions.ValidationException("La recherche doit contenir au moins deux caractères.", "search");
            if (page < 0) throw new mg.lagrace.api.exceptions.ValidationException("La page doit être positive ou nulle.", "page");
            if (size < 1 || size > 100) throw new mg.lagrace.api.exceptions.ValidationException("La taille doit être comprise entre 1 et 100.", "size");
            if (page > Integer.MAX_VALUE / size) throw new mg.lagrace.api.exceptions.ValidationException("La page demandée est trop grande.", "page");
            String term = search == null || search.isBlank() ? null : search.trim();
            SearchMode mode = term == null ? SearchMode.TEXT : term.matches("\\d+") ? SearchMode.PHONE
                : term.contains("@") ? SearchMode.EMAIL : SearchMode.TEXT;
            List<String> words = mode == SearchMode.TEXT && term != null
                ? Arrays.stream(normalize(term).split("\\s+")).filter(word -> !word.isBlank()).toList() : List.of();
            boolean activeOnly = active == null || active;
            long count = directory.count(walkInCustomerId, activeOnly, tagId, hasBalance, term, mode, words);
            List<CustomerListResponse.Item> items = directory.findItems(walkInCustomerId, activeOnly, tagId, hasBalance,
                term, mode, words, page * size, size, sort);
            CustomerListResponse.Totals totals = directory.totals(walkInCustomerId, activeOnly, tagId, hasBalance,
                term, mode, words);
            return new CustomerListResponse(items, totals, page, size, count, (int) Math.ceil((double) count / size));
            }

            @Transactional(readOnly = true)
            public List<CustomerTagResponse> customerTags() {
                return jdbc.query("select id_tag, name, color_code, is_active from customer_tag order by name",
                    (rs, row) -> new CustomerTagResponse(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
            }

            @Transactional
            public CustomerTagResponse createTag(CustomerMutationRequests.CustomerTag request) {
                String name = request.name().trim();
                ensureUniqueTagName(name, null);
                Long id = jdbc.queryForObject("""
                    insert into customer_tag(name, color_code, is_active) values (?, ?, ?) returning id_tag
                    """, Long.class, name, request.colorCode(), request.isActive());
                return customerTag(id);
            }

            @Transactional
            public CustomerTagResponse updateTag(Long id, CustomerMutationRequests.CustomerTag request) {
                if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from customer_tag where id_tag=?)", Boolean.class, id)))
                    throw new ResourceNotFoundException("Étiquette introuvable.");
                String name = request.name().trim();
                ensureUniqueTagName(name, id);
                jdbc.update("update customer_tag set name=?, color_code=?, is_active=? where id_tag=?",
                    name, request.colorCode(), request.isActive(), id);
                return customerTag(id);
            }

            private CustomerTagResponse customerTag(Long id) {
                return jdbc.queryForObject("select id_tag, name, color_code, is_active from customer_tag where id_tag=?",
                    (rs, row) -> new CustomerTagResponse(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)), id);
            }

            private void ensureUniqueTagName(String name, Long excludingId) {
                Boolean duplicate = excludingId == null
                    ? jdbc.queryForObject("select exists(select 1 from customer_tag where lower(name)=lower(?))", Boolean.class, name)
                    : jdbc.queryForObject("select exists(select 1 from customer_tag where lower(name)=lower(?) and id_tag<>?)", Boolean.class, name, excludingId);
                if (Boolean.TRUE.equals(duplicate)) throw new ConflictException("DUPLICATE_TAG", "Ce nom d’étiquette existe déjà.");
            }

            private String normalize(String value) {
            return MARKS.matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
            }

    public record FileResult(CustomerFileResponse file, Long mergedInto) {}

    @Transactional(readOnly = true)
    public FileResult file(Long id) {
        var state = jdbc.query("select is_active, merged_into_id from customer where id_customer = ?", rs -> {
            if (!rs.next()) return null;
            return new CustomerState(rs.getBoolean(1), (Number) rs.getObject(2));
        }, id);
        if (state == null) throw new ResourceNotFoundException("Client introuvable avec l'id : " + id);
        if (state.mergedInto() != null) return new FileResult(null, state.mergedInto().longValue());

        CustomerFileResponse.Identity identity = jdbc.queryForObject("""
                 select c.id_customer, c.version, c.lastname, c.firstname, c.date_of_birth, c.phone, c.email,
                     c.address, n.name, c.nationality_id, d.name, c.document_id, c.identification_number, c.preferred_language,
                   c.contact_consent, c.is_active
              from customer c left join nationality n on n.id_nationality=c.nationality_id
                 left join identification_document d on d.id_document=c.document_id
             where c.id_customer=?
            """, (rs, row) -> new CustomerFileResponse.Identity(rs.getLong(1), rs.getInt(2), rs.getString(3),
                rs.getString(4), rs.getObject(5, LocalDate.class), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getString(9), number(rs.getObject(10)), rs.getString(11), number(rs.getObject(12)),
                documentNumber(rs.getString(13)), rs.getString(14), rs.getBoolean(15), rs.getBoolean(16),
                !id.equals(walkInCustomerId), canReadSensitiveData()), id);
        CustomerFileResponse.Summary summary = jdbc.queryForObject("""
            select id_customer, bookings_count, canceled_count, first_visit, last_visit, next_visit,
                   total_billed, total_paid, balance_due, refund_due, stayed_nights, services_used, completed_count
              from v_customer_summary where id_customer=?
            """, (rs, row) -> new CustomerFileResponse.Summary(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getObject(4, LocalDate.class), rs.getObject(5, LocalDate.class), rs.getObject(6, LocalDate.class),
                money(rs.getBigDecimal(7)), money(rs.getBigDecimal(8)), money(rs.getBigDecimal(9)), money(rs.getBigDecimal(10)),
                rs.getLong(11), strings(rs.getArray(12)), rs.getLong(13)), id);
        List<CustomerFileResponse.Tag> tags = jdbc.query("""
            select t.id_tag, t.name, t.color_code from customer_tag_link l join customer_tag t on t.id_tag=l.tag_id
             where l.customer_id=? order by t.name
            """, (rs, row) -> new CustomerFileResponse.Tag(rs.getLong(1), rs.getString(2), rs.getString(3)), id);
        List<CustomerFileResponse.PinnedNote> notes = jdbc.query("""
            select id_note, note_type, content, is_alert, created_at::text from customer_note
             where customer_id=? and archived_at is null and is_pinned order by created_at desc limit 3
            """, (rs, row) -> new CustomerFileResponse.PinnedNote(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getString(5)), id);
        LevelResult level = customerLevel(summary, tags);
        return new FileResult(new CustomerFileResponse(identity, summary, level.level(), level.reasons(), tags, notes), null);
    }

    @Transactional(readOnly = true)
    public CustomerAlertsResponse alerts(Long id) {
        FileResult result = file(id);
        if (result.mergedInto() != null)
            throw new ConflictException("CUSTOMER_MERGED", "Cette fiche client a été fusionnée.", Map.of("mergedInto", result.mergedInto()));
        CustomerFileResponse file = result.file();
        List<CustomerAlertsResponse.BalanceDueBooking> dueBookings = jdbc.query("""
            select booking_id, booking_ref, balance from v_booking_folio
             where customer_id=? and balance>0 order by balance desc, period_start desc nulls last
            """, (rs, row) -> new CustomerAlertsResponse.BalanceDueBooking(rs.getLong(1), rs.getString(2), money(rs.getBigDecimal(3))), id);
        List<CustomerAlertsResponse.AlertNote> alertNotes = jdbc.query("""
            select id_note, note_type, content, created_at from customer_note
             where customer_id=? and is_alert and archived_at is null order by created_at desc, id_note desc
            """, (rs, row) -> new CustomerAlertsResponse.AlertNote(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getObject(4, java.time.OffsetDateTime.class)), id);
        boolean hasOpenBooking = Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from booking b join booking_status s on s.id_booking_status=b.status_id
             where b.customer_id=? and s.code in ('PENDING','CONFIRMED','IN_PROGRESS'))
            """, Boolean.class, id));
        return new CustomerAlertsResponse(file.level(), file.levelReasons(), file.tags(), file.summary().balanceDue(),
            dueBookings, file.summary().refundDue(), alertNotes, file.summary().nextVisit(), hasOpenBooking);
    }

    private LevelResult customerLevel(CustomerFileResponse.Summary summary, List<CustomerFileResponse.Tag> tags) {
        List<String> reasons = new ArrayList<>();
        boolean vipTag = tags.stream().anyMatch(tag -> "vip".equalsIgnoreCase(tag.name().trim()));
        BigDecimal vipThreshold = null;
        if (vipMinTotalPaid != null && !vipMinTotalPaid.isBlank()) {
            try { vipThreshold = new BigDecimal(vipMinTotalPaid.trim()); }
            catch (NumberFormatException ignored) { vipThreshold = null; }
        }
        boolean paidVip = vipThreshold != null && summary.totalPaid().compareTo(vipThreshold) >= 0;
        if (vipTag) reasons.add("Étiquette VIP");
        if (paidVip) reasons.add("Total payé ≥ " + vipThreshold.stripTrailingZeros().toPlainString() + " Ar");
        if (!reasons.isEmpty()) return new LevelResult("VIP", reasons);
        if (summary.completedCount() >= regularMinCompleted) {
            reasons.add(summary.completedCount() + " séjours terminés");
            return new LevelResult("REGULAR", reasons);
        }
        return new LevelResult("NEW", List.of());
    }

    private record LevelResult(String level, List<String> reasons) {}

    public record MergeResult(CustomerFileResponse customer, Counts counts) {}
    public record Counts(int bookings, int invoices, int notes) {}

    @Transactional
    public CustomerFileResponse update(Long id, CustomerMutationRequests.Update request, Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        requireEditable(id, request.version());
        CustomerEditState old = customerEditState(id);
        if (!request.hasChanges())
            throw new mg.lagrace.api.exceptions.ValidationException("Au moins un champ doit être modifié.", "body");
        boolean documentIdPresent = request.has("documentId");
        boolean documentNumberPresent = request.has("identificationNumber");
        if ((documentIdPresent || documentNumberPresent) && !canReadSensitiveData())
            throw new org.springframework.security.access.AccessDeniedException("Le droit données sensibles est requis pour modifier une pièce d’identité.");
        if (documentIdPresent != documentNumberPresent)
            throw new mg.lagrace.api.exceptions.ValidationException("Le type et le numéro de pièce vont ensemble.", "documentId");

        String lastname = request.has("lastname") ? trimToNull(request.lastname()) : old.lastname();
        String firstname = request.has("firstname") ? trimToNull(request.firstname()) : old.firstname();
        if (lastname == null || lastname.length() > 100)
            throw new mg.lagrace.api.exceptions.ValidationException("Le nom est obligatoire et limité à 100 caractères.", "lastname");
        if (firstname == null || firstname.length() > 100)
            throw new mg.lagrace.api.exceptions.ValidationException("Le prénom est obligatoire et limité à 100 caractères.", "firstname");
        String email = request.has("email") ? blankToNull(request.email()) : old.email();
        if (email != null && !org.springframework.util.StringUtils.hasText(email)) email = null;
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new mg.lagrace.api.exceptions.ValidationException("L’adresse e-mail est invalide.", "email");
        String phone = request.has("phone") ? blankToNull(request.phone()) : old.phone();
        if (phone != null && phone.length() > 30)
            throw new mg.lagrace.api.exceptions.ValidationException("Le téléphone est limité à 30 caractères.", "phone");
        Long documentId = documentIdPresent ? request.documentId() : old.documentId();
        String documentNumber = documentNumberPresent ? blankToNull(request.identificationNumber()) : old.identificationNumber();
        if (documentIdPresent && ((documentId == null) != (documentNumber == null)))
            throw new mg.lagrace.api.exceptions.ValidationException("Le type et le numéro de pièce vont ensemble.", "documentId");
        if (documentIdPresent && documentId != null) {
            var duplicate = jdbc.query("""
                select id_customer, ref, concat_ws(' ', firstname, lastname) from customer
                 where document_id=? and identification_number=? and id_customer<>? limit 1
                """, rs -> rs.next() ? Map.of("customerId", rs.getLong(1), "reference", rs.getString(2), "fullName", rs.getString(3)) : null,
                documentId, documentNumber, id);
            if (duplicate != null)
                throw new ConflictException("DUPLICATE_DOCUMENT", "Cette pièce d’identité est déjà utilisée.", duplicate);
        }
        LocalDate dateOfBirth = request.has("dateOfBirth") ? request.dateOfBirth() : old.dateOfBirth();
        String address = request.has("address") ? blankToNull(request.address()) : old.address();
        Long nationalityId = request.has("nationalityId") ? request.nationalityId() : old.nationalityId();
        String preferredLanguage = request.has("preferredLanguage") ? blankToNull(request.preferredLanguage()) : old.preferredLanguage();
        boolean contactConsent = old.contactConsent();
        if (request.has("contactConsent")) {
            if (request.contactConsent() == null)
                throw new mg.lagrace.api.exceptions.ValidationException("Le consentement ne peut pas être nul.", "contactConsent");
            contactConsent = request.contactConsent();
        }

        boolean hasEffectiveChange = !Objects.equals(old.lastname(), lastname) || !Objects.equals(old.firstname(), firstname)
            || !Objects.equals(old.dateOfBirth(), dateOfBirth) || !Objects.equals(old.phone(), phone)
            || !Objects.equals(old.email(), email) || !Objects.equals(old.address(), address)
            || !Objects.equals(old.nationalityId(), nationalityId) || !Objects.equals(old.documentId(), documentId)
            || !Objects.equals(old.identificationNumber(), documentNumber)
            || !Objects.equals(old.preferredLanguage(), preferredLanguage) || old.contactConsent() != contactConsent;
        if (!hasEffectiveChange) return file(id).file();

        int updated = jdbc.update("""
            update customer set lastname=?, firstname=?, date_of_birth=?, phone=?, email=?, address=?,
                   nationality_id=?, document_id=?, identification_number=?, preferred_language=?,
                   contact_consent=?, version=version+1
             where id_customer=? and version=? and is_active and merged_into_id is null
            """, lastname, firstname, dateOfBirth, phone, email, address, nationalityId, documentId,
                documentNumber, preferredLanguage, contactConsent, id, request.version());
        if (updated != 1) throw staleCustomer();

        UUID operationId = UUID.randomUUID();
        if (request.has("lastname")) logDiff(operationId, id, "lastname", old.lastname(), lastname, request.reason(), userId);
        if (request.has("firstname")) logDiff(operationId, id, "firstname", old.firstname(), firstname, request.reason(), userId);
        if (request.has("dateOfBirth")) logDiff(operationId, id, "dateOfBirth", old.dateOfBirth(), dateOfBirth, request.reason(), userId);
        if (request.has("phone")) logDiff(operationId, id, "phone", old.phone(), phone, request.reason(), userId);
        if (request.has("email")) logDiff(operationId, id, "email", old.email(), email, request.reason(), userId);
        if (request.has("address")) logDiff(operationId, id, "address", old.address(), address, request.reason(), userId);
        if (request.has("nationalityId")) logDiff(operationId, id, "nationalityId", old.nationalityId(), nationalityId, request.reason(), userId);
        if (documentIdPresent) {
            if (!Objects.equals(old.documentId(), documentId)) logChange(operationId, id, "UPDATE", "documentId", "modifié", "modifié", request.reason(), userId);
            if (!Objects.equals(old.identificationNumber(), documentNumber)) logChange(operationId, id, "UPDATE", "identificationNumber", "modifié", "modifié", request.reason(), userId);
        }
        if (request.has("preferredLanguage")) logDiff(operationId, id, "preferredLanguage", old.preferredLanguage(), preferredLanguage, request.reason(), userId);
        if (request.has("contactConsent")) logDiff(operationId, id, "contactConsent", old.contactConsent(), contactConsent, request.reason(), userId);
        return file(id).file();
    }

    @Transactional
    public CustomerFileResponse archive(Long id, CustomerMutationRequests.Reason request, Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        requireResponsible(authentication);
        CustomerMutationState state = lockCustomer(id);
        ensureNotMerged(state);
        ensureNotWalkIn(id);
        if (!state.active()) throw new ConflictException("CUSTOMER_ARCHIVED", "Ce client est déjà archivé.");
        List<Map<String, Object>> openBookings = jdbc.query("""
            select b.id_booking, b.ref, s.code, s.name from booking b
            join booking_status s on s.id_booking_status=b.status_id
            where b.customer_id=? and s.code in ('PENDING','CONFIRMED','IN_PROGRESS') order by b.ref
            """, (rs, row) -> Map.of("bookingId", rs.getLong(1), "reference", rs.getString(2),
                "statusCode", rs.getString(3), "statusName", rs.getString(4)), id);
        if (!openBookings.isEmpty())
            throw new ConflictException("CUSTOMER_HAS_OPEN_BOOKINGS", "Le client a des dossiers en cours.", Map.of("bookings", openBookings));
        int updated = jdbc.update("update customer set is_active=false, version=version+1 where id_customer=? and version=? and is_active and merged_into_id is null", id, state.version());
        if (updated != 1) throw staleCustomer();
        logChange(UUID.randomUUID(), id, "ARCHIVE", null, "true", "false", request == null ? null : request.reason(), userId);
        return file(id).file();
    }

    @Transactional
    public CustomerFileResponse restore(Long id, Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        CustomerMutationState state = lockCustomer(id);
        ensureNotMerged(state);
        ensureNotWalkIn(id);
        if (state.active()) return file(id).file();
        int updated = jdbc.update("update customer set is_active=true, version=version+1 where id_customer=? and version=? and not is_active and merged_into_id is null", id, state.version());
        if (updated != 1) throw staleCustomer();
        logChange(UUID.randomUUID(), id, "RESTORE", null, "false", "true", null, userId);
        return file(id).file();
    }

    @Transactional
    public CustomerFileResponse replaceTags(Long customerId, CustomerMutationRequests.TagReplace request,
            Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        requireEditable(customerId, request.version());
        Set<Long> requested = new LinkedHashSet<>(request.tagIds());
        if (requested.size() != request.tagIds().size())
            throw new mg.lagrace.api.exceptions.ValidationException("Une étiquette ne peut être sélectionnée qu'une seule fois.", "tagIds");
        List<Long> current = jdbc.query("select tag_id from customer_tag_link where customer_id=? order by tag_id",
                (rs, row) -> rs.getLong(1), customerId);
        Set<Long> currentSet = new LinkedHashSet<>(current);
        Set<Long> added = new LinkedHashSet<>(requested);
        added.removeAll(currentSet);
        Set<Long> removed = new LinkedHashSet<>(currentSet);
        removed.removeAll(requested);
        if (added.isEmpty() && removed.isEmpty()) return file(customerId).file();

        for (Long tagId : requested) {
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from customer_tag where id_tag=?)", Boolean.class, tagId));
            boolean active = exists && Boolean.TRUE.equals(jdbc.queryForObject("select is_active from customer_tag where id_tag=?", Boolean.class, tagId));
            if (!exists || (!active && !currentSet.contains(tagId)))
                throw new ResourceNotFoundException("Étiquette introuvable ou inactive.");
        }

        bumpCustomerVersion(customerId, request.version());
        UUID operationId = UUID.randomUUID();
        for (Long tagId : removed) {
            String name = jdbc.queryForObject("select name from customer_tag where id_tag=?", String.class, tagId);
            jdbc.update("delete from customer_tag_link where customer_id=? and tag_id=?", customerId, tagId);
            logChange(operationId, customerId, "TAG", "tag:" + tagId, name, null, null, userId);
        }
        for (Long tagId : added) {
            String name = jdbc.queryForObject("select name from customer_tag where id_tag=?", String.class, tagId);
            jdbc.update("insert into customer_tag_link(customer_id, tag_id, assigned_by_user_id) values (?, ?, ?)", customerId, tagId, userId);
            logChange(operationId, customerId, "TAG", "tag:" + tagId, null, name, null, userId);
        }
        return file(customerId).file();
    }

    @Transactional(readOnly = true)
    public List<CustomerNoteResponse> notes(Long customerId, boolean archived) {
        verifyCustomer(customerId);
        return jdbc.query("""
            select n.id_note, n.note_type, n.content, n.is_pinned, n.is_alert, n.version,
                   concat_ws(' ', u.firstname, u.lastname), n.created_at, n.updated_at, n.archived_at
              from customer_note n left join users u on u.id_user=n.created_by_user_id
             where n.customer_id=? and ((? = true and n.archived_at is not null) or (? = false and n.archived_at is null))
             order by n.is_pinned desc, n.created_at desc
            """, (rs, row) -> new CustomerNoteResponse(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getBoolean(4), rs.getBoolean(5), rs.getInt(6), rs.getString(7),
                rs.getObject(8, java.time.OffsetDateTime.class), rs.getObject(9, java.time.OffsetDateTime.class),
                rs.getObject(10, java.time.OffsetDateTime.class)), customerId, archived, archived);
    }

    @Transactional
    public CustomerNoteResponse addNote(Long customerId, CustomerMutationRequests.NoteCreate request, Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        requireNotesWritable(customerId);
        validateNoteType(request.noteType());
        String content = noteContent(request.content());
        Long noteId = jdbc.queryForObject("""
            insert into customer_note(customer_id, note_type, content, is_pinned, is_alert, created_by_user_id)
            values (?, ?, ?, ?, ?, ?) returning id_note
            """, Long.class, customerId, request.noteType(), content, Boolean.TRUE.equals(request.isPinned()),
                Boolean.TRUE.equals(request.isAlert()), userId);
        logChange(UUID.randomUUID(), customerId, "NOTE", "note:" + noteId, null, "note créée", null, userId);
        return note(noteId, customerId);
    }

    @Transactional
    public CustomerNoteResponse updateNote(Long noteId, CustomerMutationRequests.NoteUpdate request, Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        Long customerId = customerIdForNote(noteId);
        requireNotesWritable(customerId);
        CustomerNoteState old = lockNote(noteId);
        if (old.archivedAt() != null) throw new ConflictException("STALE_VERSION", "Une note archivée ne peut pas être modifiée.");
        validateNoteType(request.noteType());
        String content = noteContent(request.content());
        if (old.version() != request.version()) throw staleCustomer();
        int updated = jdbc.update("""
            update customer_note set note_type=?, content=?, is_pinned=?, is_alert=?, version=version+1
             where id_note=? and version=? and archived_at is null
            """, request.noteType(), content, Boolean.TRUE.equals(request.isPinned()),
                Boolean.TRUE.equals(request.isAlert()), noteId, request.version());
        if (updated != 1) throw staleCustomer();
        logChange(UUID.randomUUID(), old.customerId(), "NOTE", "note:" + noteId, "note", "note modifiée", null, userId);
        return note(noteId, old.customerId());
    }

    @Transactional
    public CustomerNoteResponse archiveNote(Long noteId, CustomerMutationRequests.NoteVersion request, boolean restore,
            Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        Long customerId = customerIdForNote(noteId);
        requireNotesWritable(customerId);
        CustomerNoteState note = lockNote(noteId);
        if (note.version() != request.version()) throw staleCustomer();
        if (restore == (note.archivedAt() == null)) throw new ConflictException("STALE_VERSION", "L'état de cette note a déjà changé.");
        int updated = jdbc.update("""
            update customer_note set archived_at=case when ? then null else current_timestamp end,
                   archived_by_user_id=case when ? then null else ? end, version=version+1
             where id_note=? and version=?
            """, restore, restore, userId, noteId, request.version());
        if (updated != 1) throw staleCustomer();
        logChange(UUID.randomUUID(), note.customerId(), "NOTE", "note:" + noteId, "note",
                restore ? "note restaurée" : "note archivée", null, userId);
        return note(noteId, note.customerId());
    }

    @Transactional(readOnly = true)
    public CustomerChangesResponse changes(Long customerId, int page, int size) {
        verifyCustomer(customerId);
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size)
            throw new mg.lagrace.api.exceptions.ValidationException("Pagination invalide.", "page");
        long total = jdbc.queryForObject("select count(distinct operation_id) from customer_change_log where customer_id=?", Long.class, customerId);
        List<CustomerChangeResponse> rows = jdbc.query("""
            with page_operations as (
                select operation_id, max(changed_at) as operation_at, max(id_change) as last_change
                  from customer_change_log where customer_id=? group by operation_id
                 order by operation_at desc, last_change desc limit ? offset ?
            )
            select l.id_change, l.operation_id, l.action, l.field_name, l.old_value, l.new_value,
                   l.reason, concat_ws(' ', u.firstname, u.lastname), l.changed_at
              from customer_change_log l join page_operations p on p.operation_id=l.operation_id
              left join users u on u.id_user=l.changed_by_user_id
             where l.customer_id=?
             order by p.operation_at desc, p.last_change desc, l.changed_at, l.id_change
            """, (rs, row) -> new CustomerChangeResponse(rs.getLong(1), rs.getObject(2, UUID.class),
                rs.getString(3), rs.getString(4), changeValue(rs.getString(4), rs.getString(5)),
                changeValue(rs.getString(4), rs.getString(6)), rs.getString(7), rs.getString(8),
                rs.getObject(9, java.time.OffsetDateTime.class)), customerId, size, page * size, customerId);
        Map<UUID, List<CustomerChangesResponse.FieldChange>> fields = new java.util.LinkedHashMap<>();
        Map<UUID, CustomerChangeResponse> firstRows = new java.util.LinkedHashMap<>();
        for (CustomerChangeResponse row : rows) {
            firstRows.putIfAbsent(row.operationId(), row);
            fields.computeIfAbsent(row.operationId(), ignored -> new ArrayList<>())
                .add(new CustomerChangesResponse.FieldChange(row.fieldName(), row.oldValue(), row.newValue()));
        }
        List<CustomerChangesResponse.Operation> operations = firstRows.entrySet().stream()
            .map(entry -> {
                CustomerChangeResponse row = entry.getValue();
                return new CustomerChangesResponse.Operation(row.operationId(), row.action(), row.changedAt(),
                    row.changedBy(), row.reason(), fields.get(row.operationId()));
            }).toList();
        return new CustomerChangesResponse(operations, page, size, total, (int) Math.ceil((double) total / size));
    }

    @Transactional
    public MergeResult merge(Long keptId, CustomerMutationRequests.Merge request, Authentication authentication) {
        Long userId = authenticatedUserId(authentication);
        requireResponsible(authentication);
        Long mergedId = request.mergedId();
        if (keptId.equals(mergedId)) throw mergeNotAllowed();
        long first = Math.min(keptId, mergedId);
        long second = Math.max(keptId, mergedId);
        CustomerMutationState firstState = lockCustomer(first);
        CustomerMutationState secondState = lockCustomer(second);
        CustomerMutationState keptState = keptId.equals(first) ? firstState : secondState;
        CustomerMutationState mergedState = mergedId.equals(first) ? firstState : secondState;
        if (keptId.equals(walkInCustomerId) || mergedId.equals(walkInCustomerId)
                || !keptState.active() || !mergedState.active()
                || keptState.mergedInto() != null || mergedState.mergedInto() != null) throw mergeNotAllowed();
        checkVersion(keptState.version(), request.keptVersion());
        checkVersion(mergedState.version(), request.mergedVersion());

        CustomerEditState kept = customerEditState(keptId);
        CustomerEditState merged = customerEditState(mergedId);
        CustomerMutationRequests.MergeFieldChoices choices = request.fieldChoices();
        String phone = chooseContact(kept.phone(), merged.phone(), choices == null ? null : choices.phone());
        String email = chooseContact(kept.email(), merged.email(), choices == null ? null : choices.email());
        String address = chooseContact(kept.address(), merged.address(), choices == null ? null : choices.address());
        LocalDate dateOfBirth = chooseValue(kept.dateOfBirth(), merged.dateOfBirth(), choices == null ? null : choices.dateOfBirth());
        Long nationalityId = chooseValue(kept.nationalityId(), merged.nationalityId(), choices == null ? null : choices.nationality());
        String preferredLanguage = chooseContact(kept.preferredLanguage(), merged.preferredLanguage(), choices == null ? null : choices.preferredLanguage());

        boolean chooseMergedDocument = choices != null && choices.documentType() == CustomerMutationRequests.MergeFieldChoice.MERGED;
        boolean transferDocument = chooseMergedDocument || (kept.documentId() == null && merged.documentId() != null);
        Long documentId = transferDocument ? merged.documentId() : kept.documentId();
        String identificationNumber = transferDocument ? merged.identificationNumber() : kept.identificationNumber();
        if (transferDocument && merged.documentId() != null) {
            int released = jdbc.update("""
                update customer set document_id=null, identification_number=null
                 where id_customer=? and version=? and is_active and merged_into_id is null
                """, mergedId, request.mergedVersion());
            if (released != 1) throw staleCustomer();
        }

        UUID operationId = UUID.randomUUID();
        if (transferDocument && !Objects.equals(kept.documentId(), documentId)) {
            int releasedKeptDocument = jdbc.update("update customer set document_id=null, identification_number=null where id_customer=? and version=?", keptId, request.keptVersion());
            if (releasedKeptDocument != 1) throw staleCustomer();
        }
        int keptUpdated = jdbc.update("""
            update customer set phone=?, email=?, address=?, date_of_birth=?, nationality_id=?,
                   document_id=?, identification_number=?, preferred_language=?, version=version+1
             where id_customer=? and version=? and is_active and merged_into_id is null
            """, phone, email, address, dateOfBirth, nationalityId, documentId, identificationNumber,
                preferredLanguage, keptId, request.keptVersion());
        if (keptUpdated != 1) throw staleCustomer();

        int movedBookings = jdbc.update("update booking set customer_id=? where customer_id=?", keptId, mergedId);
        int movedInvoices = jdbc.update("update invoice set customer_id=? where customer_id=?", keptId, mergedId);
        int movedNotes = jdbc.update("update customer_note set customer_id=?, version=version+1 where customer_id=?", keptId, mergedId);
        jdbc.update("""
            insert into customer_tag_link(customer_id, tag_id, assigned_by_user_id)
            select ?, tag_id, assigned_by_user_id from customer_tag_link where customer_id=?
            on conflict (customer_id, tag_id) do nothing
            """, keptId, mergedId);
        jdbc.update("delete from customer_tag_link where customer_id=?", mergedId);
        int mergedUpdated = jdbc.update("""
            update customer set is_active=false, merged_into_id=?, version=version+1
             where id_customer=? and version=? and is_active and merged_into_id is null
            """, keptId, mergedId, request.mergedVersion());
        if (mergedUpdated != 1) throw staleCustomer();
        jdbc.update("""
            insert into customer_merge_log(kept_customer_id, merged_customer_id, moved_bookings, moved_invoices,
                   moved_notes, reason, merged_by_user_id)
            values (?, ?, ?, ?, ?, ?, ?)
            """, keptId, mergedId, movedBookings, movedInvoices, movedNotes, request.reason().trim(), userId);
        logChange(operationId, keptId, "MERGE", "mergedCustomerId", null, String.valueOf(mergedId), request.reason().trim(), userId);
        logChange(operationId, mergedId, "MERGE", "mergedIntoId", null, String.valueOf(keptId), request.reason().trim(), userId);
        logMergeDiff(operationId, keptId, "phone", kept.phone(), phone, request.reason().trim(), userId);
        logMergeDiff(operationId, keptId, "email", kept.email(), email, request.reason().trim(), userId);
        logMergeDiff(operationId, keptId, "address", kept.address(), address, request.reason().trim(), userId);
        logMergeDiff(operationId, keptId, "dateOfBirth", kept.dateOfBirth(), dateOfBirth, request.reason().trim(), userId);
        logMergeDiff(operationId, keptId, "nationalityId", kept.nationalityId(), nationalityId, request.reason().trim(), userId);
        logMergeDiff(operationId, keptId, "preferredLanguage", kept.preferredLanguage(), preferredLanguage, request.reason().trim(), userId);
        if (transferDocument && !Objects.equals(kept.documentId(), documentId))
            logChange(operationId, keptId, "MERGE", "documentType", "modifié", "modifié", request.reason().trim(), userId);
        return new MergeResult(file(keptId).file(), new Counts(movedBookings, movedInvoices, movedNotes));
    }

    private static String chooseContact(String kept, String merged, CustomerMutationRequests.MergeFieldChoice choice) {
        if (choice == CustomerMutationRequests.MergeFieldChoice.MERGED) return merged;
        return kept == null || kept.isBlank() ? merged : kept;
    }

    private static <T> T chooseValue(T kept, T merged, CustomerMutationRequests.MergeFieldChoice choice) {
        if (choice == CustomerMutationRequests.MergeFieldChoice.MERGED) return merged;
        return kept == null ? merged : kept;
    }

    private void logMergeDiff(UUID operationId, Long customerId, String field, Object oldValue, Object newValue,
            String reason, Long userId) {
        if (!Objects.equals(oldValue, newValue))
            logChange(operationId, customerId, "MERGE", field, text(oldValue), text(newValue), reason, userId);
    }

    private CustomerNoteResponse note(Long noteId, Long customerId) {
        return jdbc.queryForObject("""
            select n.id_note, n.note_type, n.content, n.is_pinned, n.is_alert, n.version,
                     concat_ws(' ', u.firstname, u.lastname), n.created_at, n.updated_at, n.archived_at
              from customer_note n left join users u on u.id_user=n.created_by_user_id
             where n.id_note=? and n.customer_id=?
            """, (rs, row) -> new CustomerNoteResponse(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getBoolean(4), rs.getBoolean(5), rs.getInt(6), rs.getString(7),
                rs.getObject(8, java.time.OffsetDateTime.class), rs.getObject(9, java.time.OffsetDateTime.class),
                rs.getObject(10, java.time.OffsetDateTime.class)), noteId, customerId);
    }

    private CustomerEditState customerEditState(Long id) {
        return jdbc.queryForObject("""
            select lastname, firstname, date_of_birth, phone, email, address, nationality_id, document_id,
                   identification_number, preferred_language, contact_consent
              from customer where id_customer=?
            """, (rs, row) -> new CustomerEditState(rs.getString(1), rs.getString(2), rs.getObject(3, LocalDate.class),
                rs.getString(4), rs.getString(5), rs.getString(6), number(rs.getObject(7)), number(rs.getObject(8)),
                rs.getString(9), rs.getString(10), rs.getBoolean(11)), id);
    }

    private CustomerMutationState lockCustomer(Long id) {
        CustomerMutationState state = jdbc.query("select is_active, merged_into_id, version from customer where id_customer=? for update",
            rs -> rs.next() ? new CustomerMutationState(rs.getBoolean(1), number(rs.getObject(2)), rs.getInt(3)) : null, id);
        if (state == null) throw new ResourceNotFoundException("Client introuvable avec l'id : " + id);
        return state;
    }

    private CustomerNoteState lockNote(Long noteId) {
        CustomerNoteState state = jdbc.query("""
            select customer_id, note_type, content, is_pinned, is_alert, version, archived_at from customer_note
             where id_note=? for update
            """, rs -> rs.next() ? new CustomerNoteState(rs.getLong(1), rs.getString(2), rs.getString(3),
                    rs.getBoolean(4), rs.getBoolean(5), rs.getInt(6), rs.getObject(7, java.time.OffsetDateTime.class)) : null,
            noteId);
        if (state == null) throw new ResourceNotFoundException("Note introuvable.");
        return state;
    }

    private Long customerIdForNote(Long noteId) {
        Long customerId = jdbc.query("select customer_id from customer_note where id_note=?",
                rs -> rs.next() ? rs.getLong(1) : null, noteId);
        if (customerId == null) throw new ResourceNotFoundException("Note introuvable.");
        return customerId;
    }

    private void requireNotesWritable(Long customerId) {
        CustomerMutationState state = lockCustomer(customerId);
        ensureNotWalkIn(customerId);
        if (!state.active() || state.mergedInto() != null)
            throw new ConflictException("CUSTOMER_ARCHIVED", "Les notes sont en lecture seule pour ce client.");
    }

    private static String noteContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length()) > 2000)
            throw new mg.lagrace.api.exceptions.ValidationException("La note doit contenir entre 1 et 2000 caractères.", "content");
        return normalized;
    }

    private void requireEditable(Long id, Integer version) {
        CustomerMutationState state = lockCustomer(id);
        ensureNotMerged(state);
        ensureNotWalkIn(id);
        if (!state.active()) throw new ConflictException("CUSTOMER_ARCHIVED", "Un client archivé doit être restauré avant modification.");
        checkVersion(state.version(), version);
    }

    private void ensureNotMerged(CustomerMutationState state) {
        if (state.mergedInto() != null) throw new ConflictException("CUSTOMER_MERGED", "Cette fiche client a été fusionnée.");
    }

    private void ensureNotWalkIn(Long id) {
        if (id.equals(walkInCustomerId)) throw new BusinessException("MERGE_NOT_ALLOWED", "Le Client de passage ne peut pas être modifié.");
    }

    private void checkVersion(int current, Integer requested) {
        if (requested == null || current != requested) throw staleCustomer();
    }

    private ConflictException staleCustomer() {
        return new ConflictException("STALE_VERSION", "La fiche client a été modifiée par quelqu'un d'autre.");
    }

    private ConflictException mergeNotAllowed() {
        return new ConflictException("MERGE_NOT_ALLOWED", "Cette fusion n'est pas autorisée.");
    }

    private void bumpCustomerVersion(Long id, Integer expectedVersion) {
        int updated = jdbc.update("update customer set version=version+1 where id_customer=? and version=? and is_active and merged_into_id is null", id, expectedVersion);
        if (updated != 1) throw staleCustomer();
    }

    private Long authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new org.springframework.security.access.AccessDeniedException("Authentification requise.");
        return userRepository.findByUsername(authentication.getName()).map(mg.lagrace.api.models.User::getIdUser)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
    }

    private void requireResponsible(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(authority -> {
            String name = authority.getAuthority();
            return name.contains("Responsable") || name.contains("Administrateur");
        })) throw new org.springframework.security.access.AccessDeniedException("Archivage réservé au responsable.");
    }

    private void validateNoteType(String type) {
        if (!List.of("NOTE", "PREFERENCE", "COMPLAINT", "INCIDENT").contains(type))
            throw new mg.lagrace.api.exceptions.ValidationException("Type de note invalide.", "noteType");
    }

    private void logDiff(UUID operationId, Long customerId, String field, Object oldValue, Object newValue,
            String reason, Long userId) {
        if (!Objects.equals(oldValue, newValue))
            logChange(operationId, customerId, "UPDATE", field, text(oldValue), text(newValue), reason, userId);
    }

    private void logChange(UUID operationId, Long customerId, String action, String field, String oldValue,
            String newValue, String reason, Long userId) {
        jdbc.update("""
            insert into customer_change_log(operation_id, customer_id, action, field_name, old_value, new_value, reason, changed_by_user_id)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            """, operationId, customerId, action, field, oldValue, newValue, reason, userId);
    }

    private static String text(Object value) { return value == null ? null : String.valueOf(value); }
    private static Long number(Object value) { return value == null ? null : ((Number) value).longValue(); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String trimToNull(String value) { return value == null ? null : value.trim(); }

    private record CustomerMutationState(boolean active, Long mergedInto, int version) {}
    private record CustomerEditState(String lastname, String firstname, LocalDate dateOfBirth, String phone,
            String email, String address, Long nationalityId, Long documentId, String identificationNumber,
            String preferredLanguage, boolean contactConsent) {}
        private record CustomerNoteState(Long customerId, String noteType, String content, boolean pinned, boolean alert,
            int version, java.time.OffsetDateTime archivedAt) {}

    @Transactional(readOnly = true)
    public CustomerBookingsResponse bookings(Long customerId, String status, String from, String to, String service,
            int page, int size, String sort) {
        if (!customerRepository.existsById(customerId)) throw new ResourceNotFoundException("Client introuvable avec l'id : " + customerId);
        int safeSize = Math.max(1, Math.min(size, 100)); int safePage = Math.max(0, page);
        String order = switch (sort == null ? "" : sort) {
            case "periodStart,asc" -> "period_start asc nulls last";
            case "total,desc" -> "total desc";
            case "total,asc" -> "total asc";
            default -> "period_start desc nulls last";
        };
        String where = " customer_id=? and (? is null or status_code=?) and (? is null or period_end >= cast(? as date))"
            + " and (? is null or period_start <= cast(? as date)) and (? is null or ? = any(services)) ";
        Object[] args = { customerId, status, status, from, from, to, to, service, service };
        var totals = jdbc.queryForObject("select count(*), coalesce(sum(total),0), coalesce(sum(paid),0), coalesce(sum(balance),0) from v_booking_folio where" + where,
            (rs, row) -> new CustomerBookingsResponse.Totals(rs.getLong(1), money(rs.getBigDecimal(2)), money(rs.getBigDecimal(3)), money(rs.getBigDecimal(4))), args);
        List<Object> paged = new ArrayList<>(); java.util.Collections.addAll(paged, args); paged.add(safeSize); paged.add(safePage * safeSize);
        List<CustomerBookingsResponse.Item> items = jdbc.query("select booking_id, booking_ref, period_start, period_end, services, status_code, status_name, origin, total, paid, balance from v_booking_folio where"
            + where + " order by " + order + " limit ? offset ?", (rs, row) -> new CustomerBookingsResponse.Item(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class), strings(rs.getArray(5)), rs.getString(6), rs.getString(7), rs.getString(8), money(rs.getBigDecimal(9)), money(rs.getBigDecimal(10)), money(rs.getBigDecimal(11))), paged.toArray());
        return new CustomerBookingsResponse(items, totals, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public CustomerInvoicesResponse invoices(Long customerId, String from, String to, int page, int size) {
        verifyCustomer(customerId); int safePage = validPage(page); int safeSize = validSize(size);
        String where = " customer_id=? and (? is null or issue_date >= cast(? as date)) and (? is null or issue_date <= cast(? as date)) ";
        Object[] args = { customerId, from, from, to, to };
        long count = jdbc.queryForObject("select count(*) from v_invoice_summary where" + where, Long.class, args);
        List<Object> paged = withPaging(args, safeSize, safePage);
        List<CustomerInvoicesResponse.Item> items = jdbc.query("""
            select id_invoice, invoice_number, issue_date, booking_id, total, net_total, payment_status, status_code, invoice_type
            from v_invoice_summary where""" + where + " order by issue_date desc, id_invoice desc limit ? offset ?",
            (rs, row) -> new CustomerInvoicesResponse.Item(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class),
                rs.getObject(4, Long.class), money(rs.getBigDecimal(5)), money(rs.getBigDecimal(6)), rs.getString(7), rs.getString(8), rs.getString(9)), paged.toArray());
        return new CustomerInvoicesResponse(items, safePage, safeSize, count);
    }

    @Transactional(readOnly = true)
    public CustomerPaymentsResponse payments(Long customerId, String from, String to, int page, int size) {
        verifyCustomer(customerId); int safePage = validPage(page); int safeSize = validSize(size);
        String where = " b.customer_id=? and (? is null or p.paid_at::date >= cast(? as date)) and (? is null or p.paid_at::date <= cast(? as date)) ";
        Object[] args = { customerId, from, from, to, to };
        long count = jdbc.queryForObject("select count(*) from payment p join booking b on b.id_booking=p.booking_id where" + where, Long.class, args);
        CustomerPaymentsResponse.Totals totals = jdbc.queryForObject("""
            select coalesce(sum(case when k.code not in ('REFUND','DEPOSIT_REFUND') then p.amount else 0 end),0),
                 coalesce(sum(case when k.code in ('REFUND','DEPOSIT_REFUND') then p.amount else 0 end),0),
                 coalesce(sum(case when k.code not in ('REFUND','DEPOSIT_REFUND') then p.amount else -p.amount end),0)
            from payment p join booking b on b.id_booking=p.booking_id join payment_kind k on k.id_kind=p.kind_id where""" + where,
             (rs, row) -> new CustomerPaymentsResponse.Totals(money(rs.getBigDecimal(1)), money(rs.getBigDecimal(2)), money(rs.getBigDecimal(3))), args);
        List<Object> paged = withPaging(args, safeSize, safePage);
        List<CustomerPaymentsResponse.Item> items = jdbc.query("""
            select p.id_payment, p.paid_at, b.id_booking, b.ref, k.name, m.name,
                   case when k.code in ('REFUND','DEPOSIT_REFUND') then -p.amount else p.amount end,
                   concat_ws(' ', u.firstname, u.lastname), p.note
            from payment p join booking b on b.id_booking=p.booking_id join payment_kind k on k.id_kind=p.kind_id
            join payment_method m on m.id_method=p.method_id left join users u on u.id_user=p.received_by_user_id where""" + where + " order by p.paid_at desc, p.id_payment desc limit ? offset ?",
            (rs, row) -> new CustomerPaymentsResponse.Item(rs.getLong(1), rs.getObject(2, java.time.OffsetDateTime.class), rs.getLong(3), rs.getString(4), rs.getString(5), rs.getString(6), money(rs.getBigDecimal(7)), rs.getString(8), rs.getString(9)), paged.toArray());
        return new CustomerPaymentsResponse(items, totals, safePage, safeSize, count);
    }

    private void verifyCustomer(Long id) { if (!customerRepository.existsById(id)) throw new ResourceNotFoundException("Client introuvable avec l'id : " + id); }
    private static int validPage(int page) { return Math.max(0, page); }
    private static int validSize(int size) { return Math.max(1, Math.min(size, 100)); }
    private static List<Object> withPaging(Object[] args, int size, int page) { List<Object> values = new ArrayList<>(); java.util.Collections.addAll(values, args); values.add(size); values.add(page * size); return values; }

    private String documentNumber(String value) {
        if (value == null || canReadSensitiveData()) return value;
        return "••••••• " + (value.length() <= 4 ? value : value.substring(value.length() - 4));
    }
    private String changeValue(String field, String value) {
        return "identificationNumber".equals(field) ? documentNumber(value) : value;
    }
    private boolean canReadSensitiveData() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(a -> List.of("ROLE_ADMIN", "ROLE_DONNEES_SENSIBLES", "ROLE_DONNÉES_SENSIBLES").contains(a.getAuthority())); }
    private static BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static List<String> strings(Array array) throws java.sql.SQLException { if (array == null) return List.of(); Object[] values=(Object[]) array.getArray(); List<String> result=new ArrayList<>(); for (Object value:values) result.add(String.valueOf(value)); return result; }
    private record CustomerState(boolean active, Number mergedInto) {}
}
