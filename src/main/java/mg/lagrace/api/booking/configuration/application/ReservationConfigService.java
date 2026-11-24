package mg.lagrace.api.booking.configuration.application;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.payment.Payment;

import java.util.List;

import org.springframework.stereotype.Service;

import mg.lagrace.api.billing.InvoiceStatus;
import mg.lagrace.api.booking.configuration.dto.CodeNameRequest;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.payment.PaymentMethod;
import mg.lagrace.api.billing.persistence.InvoiceStatusRepository;
import mg.lagrace.api.payment.persistence.PaymentMethodRepository;

@Service
public class ReservationConfigService {
    private final PaymentMethodRepository paymentMethods;
    private final InvoiceStatusRepository invoiceStatuses;

    public ReservationConfigService(PaymentMethodRepository paymentMethods,
            InvoiceStatusRepository invoiceStatuses) {
        this.paymentMethods = paymentMethods;
        this.invoiceStatuses = invoiceStatuses;
    }

    public List<PaymentMethod> getPaymentMethods() { return paymentMethods.findAll(); }
    public PaymentMethod addPaymentMethod(CodeNameRequest request) {
        String value = name(request == null ? null : request.name(), "du moyen de paiement");
        if (paymentMethods.existsByName(value)) throw new DuplicateResourceException("Le moyen de paiement existe déjà.");
        PaymentMethod method = new PaymentMethod(); method.setName(value); return paymentMethods.save(method);
    }
    public PaymentMethod updatePaymentMethod(Long id, CodeNameRequest request) {
        PaymentMethod method = find(paymentMethods, id, "Moyen de paiement");
        String value = name(request == null ? null : request.name(), "du moyen de paiement");
        if (!method.getName().equals(value) && paymentMethods.existsByName(value)) throw new DuplicateResourceException("Le moyen de paiement existe déjà.");
        method.setName(value); return paymentMethods.save(method);
    }
    public void deletePaymentMethod(Long id) { paymentMethods.delete(find(paymentMethods, id, "Moyen de paiement")); }

    public List<InvoiceStatus> getInvoiceStatuses() { return invoiceStatuses.findAll(); }
    public InvoiceStatus addInvoiceStatus(CodeNameRequest request) {
        String code = code(request == null ? null : request.code(), "du statut de facture");
        String name = name(request == null ? null : request.name(), "du statut de facture");
        if (invoiceStatuses.existsByCode(code) || invoiceStatuses.existsByName(name)) throw new DuplicateResourceException("Le statut de facture existe déjà.");
        InvoiceStatus status = new InvoiceStatus(); status.setCode(code); status.setName(name); return invoiceStatuses.save(status);
    }
    public InvoiceStatus updateInvoiceStatus(Long id, CodeNameRequest request) {
        InvoiceStatus status = find(invoiceStatuses, id, "Statut de facture");
        String code = code(request == null ? null : request.code(), "du statut de facture");
        String name = name(request == null ? null : request.name(), "du statut de facture");
        if ((!status.getCode().equals(code) && invoiceStatuses.existsByCode(code)) || (!status.getName().equals(name) && invoiceStatuses.existsByName(name))) throw new DuplicateResourceException("Le statut de facture existe déjà.");
        status.setCode(code); status.setName(name); return invoiceStatuses.save(status);
    }
    public void deleteInvoiceStatus(Long id) { invoiceStatuses.delete(find(invoiceStatuses, id, "Statut de facture")); }

    private String name(String value, String label) { if (value == null || value.isBlank()) throw new ValidationException("Le nom " + label + " est obligatoire."); return value.trim(); }
    private String code(String value, String label) { if (value == null || value.isBlank()) throw new ValidationException("Le code " + label + " est obligatoire."); return value.trim().toUpperCase(); }
    private <T> T find(org.springframework.data.jpa.repository.JpaRepository<T, Long> repo, Long id, String label) {
        if (id == null) throw new ValidationException("L'identifiant de " + label.toLowerCase() + " est obligatoire.");
        return repo.findById(id).orElseThrow(() -> notFound(label, id));
    }
    private ResourceNotFoundException notFound(String label, Long id) { return new ResourceNotFoundException(label + " introuvable avec l'id " + id); }
}
