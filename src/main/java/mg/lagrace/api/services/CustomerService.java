package mg.lagrace.api.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.customer.CustomerResponse;
import mg.lagrace.api.repositories.CustomerRepository;

@Service
public class CustomerService {
    private static final int MAX_AUTOCOMPLETE_RESULTS = 50;
    private static final int MIN_SEARCH_LENGTH = 1;

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String query, int limit) {
        if (query == null || query.isBlank() || query.trim().length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        int pageSize = Math.max(1, Math.min(limit, MAX_AUTOCOMPLETE_RESULTS));
        return customerRepository.searchForAutocomplete(query.trim(), PageRequest.of(0, pageSize))
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }
}