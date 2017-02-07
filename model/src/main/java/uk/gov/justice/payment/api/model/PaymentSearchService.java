package uk.gov.justice.payment.api.model;


import uk.gov.justice.payment.api.model.exceptions.PaymentException;
import uk.gov.justice.payment.api.model.exceptions.PaymentNotFoundException;

import java.util.List;

public interface PaymentSearchService {
    default Payment findOne(String serviceId, PaymentSearchCriteria searchCriteria) {
        List<Payment> results = find(serviceId, searchCriteria);

        if (results.isEmpty()) {
            throw new PaymentNotFoundException();
        }

        if (results.size() > 1) {
            throw new PaymentException("Expected one payment, but more than one was found");
        }

        return results.get(0);
    }

    List<Payment> find(String serviceId, PaymentSearchCriteria searchCriteria);
}
