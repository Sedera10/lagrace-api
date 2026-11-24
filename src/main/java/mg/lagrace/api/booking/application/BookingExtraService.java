package mg.lagrace.api.booking.application;

import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.reference.ServiceScope;

import mg.lagrace.api.booking.dto.room.BookingRoomRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import mg.lagrace.api.auth.User;
import mg.lagrace.api.booking.charge.BookingCharge;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.catalog.restaurant.MenuItem;
import mg.lagrace.api.catalog.restaurant.MenuItemRate;
import mg.lagrace.api.booking.dto.room.BookingRoomRequest.ExtraItem;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.catalog.additionalservice.persistence.AdditionalServiceRepository;
import mg.lagrace.api.booking.charge.persistence.BookingChargeRepository;
import mg.lagrace.api.catalog.restaurant.persistence.MenuItemRateRepository;
import mg.lagrace.api.catalog.restaurant.persistence.MenuItemRepository;

@Service
public class BookingExtraService {
    private final AdditionalServiceRepository services;
    private final MenuItemRepository menuItems;
    private final MenuItemRateRepository menuRates;
    private final BookingChargeRepository charges;

    public BookingExtraService(AdditionalServiceRepository services, MenuItemRepository menuItems,
            MenuItemRateRepository menuRates, BookingChargeRepository charges) {
        this.services = services;
        this.menuItems = menuItems;
        this.menuRates = menuRates;
        this.charges = charges;
    }

    public void addToBooking(Booking booking, List<ExtraItem> extras, User user,
            LocalDate rateDate, String serviceScope) {
        if (extras == null) return;
        for (int index = 0; index < extras.size(); index++) {
            ExtraItem extra = extras.get(index);
            String field = "extras[" + index + "]";
            if (extra == null || extra.quantity() == null || extra.quantity() < 1
                    || (extra.menuItemId() == null) == (extra.additionalServiceId() == null)) {
                throw new ValidationException("Chaque extra doit avoir une quantité positive et une seule source.", field);
            }
            BookingCharge charge = new BookingCharge();
            charge.setBooking(booking);
            charge.setCreatedByUser(user);
            charge.setQuantity(extra.quantity());
            charge.setIncluded(false);
            if (extra.additionalServiceId() != null) {
                AdditionalService service = services.findById(extra.additionalServiceId().longValue())
                        .orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable."));
                String scope = service.getScope() == null || service.getScope().getCode() == null
                        ? "" : service.getScope().getCode().toUpperCase(Locale.ROOT);
                if (!service.isActive() || service.isDeposit() || !serviceScope.equals(scope)) {
                    throw new UnprocessableEntityException("VALIDATION",
                            "Le service « " + service.getName() + " » ne s'applique pas à cette réservation.", field);
                }
                charge.setAdditionalService(service);
                charge.setLabel(service.getName());
                charge.setUnitPrice(service.getPrice());
                charge.setDeposit(false);
            } else {
                MenuItem item = menuItems.findById(extra.menuItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Article du menu introuvable."));
                if (!Boolean.TRUE.equals(item.getActive())) {
                    throw new UnprocessableEntityException("VALIDATION", "L'article du menu n'est pas actif.", field);
                }
                MenuItemRate rate = menuRates.findApplicableByMenuItem(item.getIdMenuItem(), rateDate);
                if (rate == null) {
                    throw new UnprocessableEntityException("NO_TARIFF",
                            "Aucun prix applicable pour l'article « " + item.getName() + " ».", field);
                }
                charge.setMenuItem(item);
                charge.setLabel(item.getName());
                charge.setUnitPrice(rate.getAmount());
                charge.setDeposit(false);
            }
            charges.saveAndFlush(charge);
        }
    }
}