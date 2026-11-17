package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.invoice.CreditDraftResponse;
import mg.lagrace.api.dto.invoice.CreditNoteRequest;
import mg.lagrace.api.dto.invoice.CreditNoteResponse;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.Invoice;
import mg.lagrace.api.models.InvoiceLine;
import mg.lagrace.api.models.User;
import mg.lagrace.api.models.views.InvoiceSummary;
import mg.lagrace.api.repositories.HotelSettingsRepository;
import mg.lagrace.api.repositories.InvoiceLineRepository;
import mg.lagrace.api.repositories.InvoiceRepository;
import mg.lagrace.api.repositories.InvoiceStatusRepository;
import mg.lagrace.api.repositories.InvoiceSummaryRepository;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class InvoiceCreditService {
    private static final Set<String> CREDIT_ROLES = Set.of("ROLE_Responsable", "ROLE_Administrateur");
    private static final Set<String> CREDITABLE_STATUSES = Set.of("ISSUED", "PARTIALLY_CREDITED");

    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final InvoiceStatusRepository statuses;
    private final InvoiceSummaryRepository summaries;
    private final HotelSettingsRepository settings;
    private final UserRepository users;
    private final BookingChangeLogger changes;
    private final BillingReadService billingRead;
    private final Clock clock;

    public InvoiceCreditService(InvoiceRepository invoices, InvoiceLineRepository lines,
            InvoiceStatusRepository statuses, InvoiceSummaryRepository summaries,
            HotelSettingsRepository settings, UserRepository users, BookingChangeLogger changes,
            BillingReadService billingRead, Clock clock) {
        this.invoices = invoices;
        this.lines = lines;
        this.statuses = statuses;
        this.summaries = summaries;
        this.settings = settings;
        this.users = users;
        this.changes = changes;
        this.billingRead = billingRead;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CreditDraftResponse draft(Long invoiceId, Authentication authentication) {
        requireManager(authentication);
        Invoice original = invoice(invoiceId);
        validateCreditTarget(original);
        InvoiceSummary summary = summary(invoiceId);
        List<CreditDraftResponse.Line> draftLines = lines.findAllByInvoice_IdInvoiceOrderByPositionAsc(invoiceId).stream()
                .filter(this::creditable)
                .map(line -> new CreditDraftResponse.Line(line.getIdInvoiceLine(), line.getDescription(),
                        line.getQuantity(), line.getUnitPrice(), amount(line), isDiscount(line)))
                .toList();
        if (draftLines.stream().noneMatch(line -> !line.discount()))
            throw notAllowed("Aucune ligne positive ne peut être créditée.");
        return new CreditDraftResponse(invoiceId, original.getBooking().getIdBooking(), original.getInvoiceNumber(),
            original.getBillingName(), original.getStatus().getCode(),
                value(summary.getNetTotal()), value(summary.getPaidOnInvoice()), draftLines);
    }

    @Transactional
    public CreditNoteResponse credit(Long invoiceId, CreditNoteRequest request, Authentication authentication) {
        requireManager(authentication);
        if (request == null || request.reason() == null || request.reason().isBlank())
            throw new ValidationException("Le motif de l'avoir est obligatoire.", "reason");
        if (request.reason().trim().length() > 255)
            throw new ValidationException("Le motif ne peut pas dépasser 255 caractères.", "reason");
        if (request.lineIds() == null || request.lineIds().isEmpty())
            throw new ValidationException("Sélectionnez au moins une ligne à créditer.", "lineIds");

        User user = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        Invoice original = invoices.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable avec l'id : " + invoiceId));
        validateCreditTarget(original);
        InvoiceSummary before = summary(invoiceId);
        List<InvoiceLine> originalLines = lines.findAllByInvoice_IdInvoiceOrderByPositionAsc(invoiceId);
        List<InvoiceLine> creditable = originalLines.stream().filter(this::creditable).toList();
        Set<Long> selectedIds = new HashSet<>(request.lineIds());
        if (selectedIds.size() != request.lineIds().size())
            throw new ValidationException("Une ligne ne peut être sélectionnée qu'une seule fois.", "lineIds");

        List<InvoiceLine> selected = new ArrayList<>();
        for (Long lineId : selectedIds) {
            InvoiceLine line = originalLines.stream().filter(candidate -> candidate.getIdInvoiceLine().equals(lineId))
                    .findFirst().orElseThrow(() -> notAllowed("Une ligne sélectionnée n'appartient pas à cette facture."));
            if (!creditable(line)) throw notAllowed("Une ligne sélectionnée a déjà été créditée.");
            if (isDiscount(line)) throw notAllowed("Les lignes de remise sont ajoutées automatiquement.");
            selected.add(line);
        }

        List<InvoiceLine> positiveLines = creditable.stream().filter(line -> !isDiscount(line)).toList();
        boolean allPositiveSelected = selectedIds.containsAll(positiveLines.stream()
                .map(InvoiceLine::getIdInvoiceLine).toList());
        List<InvoiceLine> creditLines = new ArrayList<>(selected);
        if (allPositiveSelected) creditable.stream().filter(this::isDiscount).forEach(creditLines::add);

        BigDecimal creditTotal = creditLines.stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (creditTotal.signum() <= 0) throw notAllowed("Le montant de l'avoir doit être positif.");
        if (creditTotal.compareTo(value(before.getNetTotal())) > 0)
            throw notAllowed("Le total dépasse le net restant : créditez aussi la remise.");
        if (request.expectedTotal() == null || request.expectedTotal().compareTo(creditTotal) != 0)
            throw new ConflictException("TOTAL_CHANGED", "Le montant de l'avoir a changé.", null, creditTotal);

        OffsetDateTime now = OffsetDateTime.now(clock);
        Invoice credit = new Invoice();
        credit.setInvoiceType("CREDIT_NOTE");
        credit.setCustomer(original.getCustomer());
        credit.setBooking(original.getBooking());
        credit.setStatus(statuses.findByCode("ISSUED")
                .orElseThrow(() -> new ResourceNotFoundException("Statut ISSUED introuvable.")));
        credit.setCreditedInvoice(original);
        credit.setCreditReason(request.reason().trim());
        credit.setIssueDate(LocalDate.now(clock));
        credit.setBillingName(original.getBillingName());
        credit.setBillingAddress(original.getBillingAddress());
        credit.setBillingPhone(original.getBillingPhone());
        credit.setBillingNationality(original.getBillingNationality());
        credit.setBillingTaxId(original.getBillingTaxId());
        credit.setReferenceLabel(original.getReferenceLabel());
        credit.setPeriodStart(original.getPeriodStart());
        credit.setPeriodEnd(original.getPeriodEnd());
        credit.setSubtotal(creditTotal);
        credit.setDiscount(BigDecimal.ZERO);
        credit.setTotal(creditTotal);
        credit.setNotes("Avoir de " + original.getInvoiceNumber() + " : " + request.reason().trim());
        credit.setCreatedByUser(user);
        credit.setIssuerSnapshot(InvoiceIssuanceService.snapshot(settings.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètres de l'hôtel introuvables."))));
        credit.setCreatedAt(now);
        credit.setUpdatedAt(now);
        credit = invoices.saveAndFlush(credit);

        int position = 1;
        try {
            for (InvoiceLine originalLine : creditLines) {
                InvoiceLine creditLine = new InvoiceLine();
                creditLine.setInvoice(credit);
                creditLine.setDescription("Avoir : " + originalLine.getDescription());
                creditLine.setQuantity(originalLine.getQuantity());
                creditLine.setUnitPrice(originalLine.getUnitPrice());
                creditLine.setPosition(position++);
                creditLine.setServiceCode(originalLine.getServiceCode());
                creditLine.setBilled(false);
                creditLine.setCreditedLine(originalLine);
                lines.save(creditLine);
                originalLine.setBilled(false);
                lines.save(originalLine);
            }
            lines.flush();
        } catch (DataIntegrityViolationException exception) {
            throw notAllowed("Une ligne a déjà été créditée par un autre utilisateur.");
        }

        boolean fullyCredited = originalLines.stream().allMatch(line -> !Boolean.TRUE.equals(line.getBilled()));
        original.setStatus(statuses.findByCode(fullyCredited ? "CREDITED" : "PARTIALLY_CREDITED")
                .orElseThrow(() -> new ResourceNotFoundException("Statut d'avoir introuvable.")));
        original.setUpdatedAt(now);
        invoices.saveAndFlush(original);
        changes.log(original.getBooking().getIdBooking(), "INVOICE", original.getIdInvoice(), "CREDIT",
                "credit_note", null, credit.getInvoiceNumber(), request.reason().trim(), user.getIdUser(), UUID.randomUUID());

        InvoiceSummary after = summary(invoiceId);
        BigDecimal refundSuggested = value(before.getPaidOnInvoice()).subtract(value(after.getNetTotal())).max(BigDecimal.ZERO);
        return new CreditNoteResponse(billingRead.invoiceDetail(credit.getIdInvoice()), refundSuggested);
    }

    private void validateCreditTarget(Invoice invoice) {
        if (!"INVOICE".equals(invoice.getInvoiceType()) || invoice.getStatus() == null
                || !CREDITABLE_STATUSES.contains(invoice.getStatus().getCode()))
            throw notAllowed("Seules les factures émises ou partiellement créditées peuvent recevoir un avoir.");
    }

    private Invoice invoice(Long id) {
        return invoices.findById(id).orElseThrow(() -> new ResourceNotFoundException("Facture introuvable avec l'id : " + id));
    }

    private InvoiceSummary summary(Long id) {
        return summaries.findById(id).orElseThrow(() -> new ResourceNotFoundException("Résumé de facture introuvable."));
    }

    private boolean creditable(InvoiceLine line) {
        return Boolean.TRUE.equals(line.getBilled()) && line.getCreditedLine() == null;
    }

    private boolean isDiscount(InvoiceLine line) { return amount(line).signum() < 0; }
    private BigDecimal amount(InvoiceLine line) {
        return line.getAmount() == null ? line.getQuantity().multiply(line.getUnitPrice()) : line.getAmount();
    }
    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private void requireManager(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> CREDIT_ROLES.contains(authority.getAuthority())))
            throw new ConflictException("FORBIDDEN", "La création d'un avoir nécessite le rôle Responsable.");
    }

    private ConflictException notAllowed(String message) { return new ConflictException("CREDIT_NOT_ALLOWED", message); }
}
