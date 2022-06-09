package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

import java.util.Optional;

public interface PaymentStatusUpdateService {

    PaymentFailures insertBounceChequePaymentFailure(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto);
    Optional<PaymentFailures> searchFailureReference(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto);

    void sendMessageToTopic(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto) throws JsonProcessingException;

    boolean cancelFailurePaymentRefund(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto);
}
