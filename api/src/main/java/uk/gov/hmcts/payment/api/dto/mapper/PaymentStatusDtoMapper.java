package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

@Component
public class PaymentStatusDtoMapper {
    private static final String BOUNCEDCHEQUE = "Bounced Cheque";
    private static final String CHARGEBACK = "Chargeback";
    public PaymentFailures bounceChequeRequestMapper(PaymentStatusBouncedChequeDto PaymentStatusBouncedChequeDto) {

        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .paymentReference(PaymentStatusBouncedChequeDto.getPaymentReference())
            .failureReference(PaymentStatusBouncedChequeDto.getFailureReference())
            .reason(PaymentStatusBouncedChequeDto.getReason())
            .ccdCaseNumber(PaymentStatusBouncedChequeDto.getCcdCaseNumber())
            .amount(PaymentStatusBouncedChequeDto.getAmount())
            .additionalReference(PaymentStatusBouncedChequeDto.getAdditionalReference())
            .failureEventDateTime(PaymentStatusBouncedChequeDto.getFailureEventDateTime())
            .failureType(BOUNCEDCHEQUE)
            .build();
        return paymentFailures;
    }

    public PaymentFailures ChargebackRequestMapper(PaymentStatusChargebackDto paymentStatusChargebackDto) {

        PaymentFailures paymentFailures = PaymentFailures.paymentFailuresWith()
            .paymentReference(paymentStatusChargebackDto.getPaymentReference())
            .failureReference(paymentStatusChargebackDto.getFailureReference())
            .reason(paymentStatusChargebackDto.getReason())
            .ccdCaseNumber(paymentStatusChargebackDto.getCcdCaseNumber())
            .amount(paymentStatusChargebackDto.getAmount())
            .additionalReference(paymentStatusChargebackDto.getAdditionalReference())
            .failureEventDateTime(paymentStatusChargebackDto.getFailureEventDateTime())
            .failureType(CHARGEBACK)
            .hasAmountDebited(paymentStatusChargebackDto.getHasAmountDebited())
            .build();
        return paymentFailures;
    }
}
