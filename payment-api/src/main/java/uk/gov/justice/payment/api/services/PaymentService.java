package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;


public interface PaymentService {

    void storePayment(CreatePaymentRequest request , GDSCreatePaymentResponse response);

}
