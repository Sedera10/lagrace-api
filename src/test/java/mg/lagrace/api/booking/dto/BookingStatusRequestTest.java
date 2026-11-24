package mg.lagrace.api.booking.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class BookingStatusRequestTest {
    @Test
    void acceptsValidOrMissingColor() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(new BookingStatusRequest("CONFIRMED", "Confirmée", "#3B82FF")).isEmpty());
            assertTrue(validator.validate(new BookingStatusRequest("CONFIRMED", "Confirmée", null)).isEmpty());
        }
    }

    @Test
    void rejectsIncompleteHexColor() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertFalse(validator.validate(new BookingStatusRequest("CONFIRMED", "Confirmée", "#3B82F")).isEmpty());
        }
    }
}
