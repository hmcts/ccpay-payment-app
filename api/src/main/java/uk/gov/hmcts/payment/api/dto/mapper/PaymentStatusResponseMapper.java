package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.PaymentFailureDto;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

@Component
public class PaymentStatusResponseMapper {

    public PaymentFailureDto toPaymentFailure(PaymentFailures paymentFailures){

        return PaymentFailureDto.paymentFailureResponseDtoWith()
            .additionalReference(paymentFailures.getAdditionalReference())
            .failureReference(paymentFailures.getFailureReference())
            .failureReason(paymentFailures.getReason())
            .paymentReference(paymentFailures.getPaymentReference())
            .disputedAmount(paymentFailures.getAmount())
            .failureType(paymentFailures.getFailureType())
            .Status(toStatus(paymentFailures))
            .hasAmountDebited(paymentFailures.getHasAmountDebited())
            .representmentOutcomeDate(paymentFailures.getRepresentmentOutcomeDate())
            .failureEventDateTime(paymentFailures.getFailureEventDateTime())
            .representmentStatus(paymentFailures.getRepresentmentSuccess())
            .build();
    }

    private String toStatus(PaymentFailures paymentFailures){

        if(null != paymentFailures.getRepresentmentSuccess()){
            return "closed";
        }
        return "initiated";
    }

}
