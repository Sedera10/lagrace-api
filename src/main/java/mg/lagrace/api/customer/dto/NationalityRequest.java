package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

public record NationalityRequest(String name, String countryCode, String flagEmoji) {
}