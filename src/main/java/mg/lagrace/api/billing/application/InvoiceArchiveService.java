package mg.lagrace.api.billing.application;

import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.Space;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.billing.dto.invoice.InvoiceArchiveResponse;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.billing.persistence.InvoiceArchiveRepository;

@Service
@Transactional(readOnly = true)
public class InvoiceArchiveService {
    private static final Set<String> PAYMENT_STATUSES = Set.of("PAID", "PARTIAL", "UNPAID", "CANCELED", "CREDIT_NOTE");
    private static final Set<String> SERVICE_CODES = Set.of("ROOM", "SPACE", "MEETING_ROOM", "POOL", "RESTAURANT", "EXTRA", "OTHER");
    private static final Set<String> INVOICE_TYPES = Set.of("INVOICE", "CREDIT_NOTE");
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InvoiceArchiveRepository repository;

    public InvoiceArchiveService(InvoiceArchiveRepository repository) { this.repository = repository; }

    public InvoiceArchiveResponse list(String search, String paymentStatus, String service, String type,
            LocalDate from, LocalDate to, int page, int size, String sort) {
        validateFilters(search, paymentStatus, service, type, from, to);
        if (page < 0) throw new ValidationException("La page doit être positive ou nulle.", "page");
        if (size < 1 || size > 100) throw new ValidationException("La taille de page doit être comprise entre 1 et 100.", "size");
        String normalizedSearch = normalize(search);
        String normalizedPaymentStatus = normalize(paymentStatus);
        String normalizedService = normalize(service);
        String normalizedType = normalize(type);
        long count = repository.count(normalizedSearch, normalizedPaymentStatus, normalizedService, normalizedType, from, to);
        List<InvoiceArchiveResponse.Item> items = repository.findItems(normalizedSearch,
                normalizedPaymentStatus, normalizedService, normalizedType, from, to, page * size, size, sort);
        InvoiceArchiveResponse.Totals totals = repository.totals(normalizedSearch,
                normalizedPaymentStatus, normalizedService, normalizedType, from, to);
        int pages = (int) Math.ceil((double) count / size);
        return new InvoiceArchiveResponse(items, totals, page, size, count, pages);
    }

    public byte[] exportCsv(String search, String paymentStatus, String service, String type,
            LocalDate from, LocalDate to, String sort) {
        validateFilters(search, paymentStatus, service, type, from, to);
        List<InvoiceArchiveResponse.Item> items = repository.exportItems(normalize(search),
                normalize(paymentStatus), normalize(service), normalize(type), from, to, sort);
        StringBuilder csv = new StringBuilder("\uFEFFN°;Date;Client;Référence;Service;Total;Net;Mode de paiement;Paiement;Statut;Type;Avoirs émis;Facture créditée\r\n");
        for (InvoiceArchiveResponse.Item item : items) {
            csv.append(cell(item.number())).append(';')
                    .append(item.issueDate() == null ? "" : CSV_DATE.format(item.issueDate())).append(';')
                    .append(cell(item.customerName())).append(';')
                    .append(cell(item.referenceLabel())).append(';')
                    .append(cell(item.serviceLabel())).append(';')
                    .append(amount(item.total())).append(';')
                    .append(amount(item.netTotal())).append(';')
                    .append(cell(item.paymentMethodLabel())).append(';')
                    .append(cell(item.paymentStatus())).append(';')
                    .append(cell(item.status())).append(';')
                    .append(cell(item.type())).append(';')
                    .append(cell(String.join(", ", item.creditedByNumbers()))).append(';')
                    .append(cell(item.creditedInvoiceNumber())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void validateFilters(String search, String paymentStatus, String service, String type,
            LocalDate from, LocalDate to) {
        if (search != null && !search.isBlank() && search.trim().length() < 2)
            throw new ValidationException("La recherche doit contenir au moins deux caractères.", "search");
        if (paymentStatus != null && !paymentStatus.isBlank() && !PAYMENT_STATUSES.contains(paymentStatus.trim().toUpperCase()))
            throw new ValidationException("Statut de paiement invalide.", "paymentStatus");
        if (service != null && !service.isBlank() && !SERVICE_CODES.contains(service.trim().toUpperCase()))
            throw new ValidationException("Code de service invalide.", "service");
        if (type != null && !type.isBlank() && !INVOICE_TYPES.contains(type.trim().toUpperCase()))
            throw new ValidationException("Type de facture invalide.", "type");
        if (from != null && to != null && to.isBefore(from))
            throw new ValidationException("La date de fin doit être postérieure ou égale à la date de début.", "to");
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(); }
    private String amount(java.math.BigDecimal value) { return value == null ? "0" : value.toPlainString(); }
    private String cell(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }
}