package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.exceptions.ApplicationException;
import uk.gov.justice.payment.api.exceptions.PaymentNotFoundException;
import uk.gov.justice.payment.api.model.PaymentDetails;

import java.util.List;

public interface PaymentSearchService {
    default PaymentDetails findOne(String serviceId, PaymentSearchCriteria searchCriteria) {
        List<PaymentDetails> results = find(serviceId, searchCriteria);

        if (results.isEmpty()) {
            throw new PaymentNotFoundException();
        }

        if (results.size() > 1) {
            throw new ApplicationException("Expected one payment, but more than one was found");
        }

        return results.get(0);
    }

    List<PaymentDetails> find(String serviceId, PaymentSearchCriteria searchCriteria);
}
