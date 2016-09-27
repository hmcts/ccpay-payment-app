package uk.gov.justice.payment.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.payment.api.domain.PaymentDetails;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;
import uk.gov.justice.payment.api.repository.PaymentRepository;

/**
 * Created by zeeshan on 27/09/2016.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentRepository paymentRepository;

    public void storePayment(CreatePaymentRequest request, CreatePaymentResponse response) {
        PaymentDetails paymentRequest = new PaymentDetails(request,response);
        paymentRepository.save(paymentRequest);
    }
}
