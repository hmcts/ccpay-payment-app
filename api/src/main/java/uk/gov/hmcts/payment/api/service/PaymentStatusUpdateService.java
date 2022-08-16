package uk.gov.hmcts.payment.api.service;

import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusUpdateSecond;
import uk.gov.hmcts.payment.api.dto.UnprocessedPayment;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

import java.util.List;

public interface PaymentStatusUpdateService {

    PaymentFailures insertBounceChequePaymentFailure(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto);
    boolean cancelFailurePaymentRefund(String paymentReference);
    PaymentFailures insertChargebackPaymentFailure(PaymentStatusChargebackDto paymentStatusChargebackDto);
    List<PaymentFailures> searchPaymentFailure(String failureReference);
    void deleteByFailureReference(String failureReference);

    PaymentFailures updatePaymentFailure(String paymentFailures, PaymentStatusUpdateSecond paymentStatusUpdateSecond);

    void updateUnprocessedPayment();

    PaymentFailures unprocessedPayment(UnprocessedPayment unprocessedPayment,
                                       MultiValueMap<String, String> headers);
}
