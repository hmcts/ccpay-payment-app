package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.TransactionRecord;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;

import java.util.List;


public interface PaymentService {

    void storePayment(CreatePaymentRequest request, GDSCreatePaymentResponse response);

    void updatePayment(String paymentId, String status);

    List<TransactionRecord> searchPayment(SearchCriteria searchCriteria);

}
