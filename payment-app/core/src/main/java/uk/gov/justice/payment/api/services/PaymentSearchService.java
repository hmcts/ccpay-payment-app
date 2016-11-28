package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.exceptions.ApplicationException;
import uk.gov.justice.payment.api.exceptions.PaymentNotFoundException;
import uk.gov.justice.payment.api.model.Payment;

import java.util.List;

public interface PaymentSearchService {
    default Payment findOne(String serviceId, PaymentSearchCriteria searchCriteria) {
        List<Payment> results = find(serviceId, searchCriteria);

        if (results.isEmpty()) {
            throw new PaymentNotFoundException();
        }

        if (results.size() > 1) {
            throw new ApplicationException("Expected one payment, but more than one was found");
        }

        return results.get(0);
    }

    List<Payment> find(String serviceId, PaymentSearchCriteria searchCriteria);
}
