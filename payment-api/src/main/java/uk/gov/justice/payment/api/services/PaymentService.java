package uk.gov.justice.payment.api.services;

import io.swagger.annotations.ApiParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;


public interface PaymentService {

    void storePayment(CreatePaymentRequest request , CreatePaymentResponse response);

}
