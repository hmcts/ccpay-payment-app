package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentStatusUpdateService {

    PaymentFailures insertBounceChequePaymentFailure(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto);
    Optional<PaymentFailures> searchFailureReference(String failureReference);

    void sendFailureMessageToServiceTopic(String paymentReference, BigDecimal amount) throws JsonProcessingException;

    boolean cancelFailurePaymentRefund(String paymentReference);

    PaymentFailures insertChargebackPaymentFailure(PaymentStatusChargebackDto paymentStatusChargebackDto);

    PaymentFailures searchPaymentFailure(String failureReference);
}
