package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

@Component
public class PaymentStatusDtoMapper {
    private static final String BOUNCEDCHEQUE = "Bounced Cheque";
    public PaymentFailures bounceChequeRequestMapper(PaymentStatusBouncedChequeDto PaymentStatusBouncedChequeDto) {

        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .paymentReference(PaymentStatusBouncedChequeDto.getPaymentReference())
            .failureReference(PaymentStatusBouncedChequeDto.getFailureReference())
            .reason(PaymentStatusBouncedChequeDto.getReason())
            .ccdCaseNumber(PaymentStatusBouncedChequeDto.getCcdCaseNumber())
            .amount(PaymentStatusBouncedChequeDto.getAmount())
            .additionalReference(PaymentStatusBouncedChequeDto.getAdditionalReference())
            .failureEventDateTime(PaymentStatusBouncedChequeDto.getFailure_event_date_time())
            .failureType(BOUNCEDCHEQUE)
            .build();
        return paymentFailures;
    }

}
