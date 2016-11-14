package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.domain.PaymentDetails;
import uk.gov.justice.payment.api.json.api.TransactionRecord;

import java.util.List;


public interface PaymentService {

    void storePayment(PaymentDetails paymentDetails);

    void updatePayment(String paymentId, String status);

    List<TransactionRecord> searchPayment(SearchCriteria searchCriteria);

}
