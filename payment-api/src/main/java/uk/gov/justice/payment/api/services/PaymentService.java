package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;


public interface PaymentService {

    void storePayment(CreatePaymentRequest request , CreatePaymentResponse response);

}
