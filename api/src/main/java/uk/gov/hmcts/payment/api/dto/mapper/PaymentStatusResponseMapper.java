package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.PaymentFailureClosedDto;
import uk.gov.hmcts.payment.api.dto.PaymentFailureInitiatedDto;
import uk.gov.hmcts.payment.api.dto.PaymentFailureResponseDto;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

@Component
public class PaymentStatusResponseMapper {


    public PaymentFailureResponseDto toPaymentFailure(PaymentFailures paymentFailure){

        PaymentFailureResponseDto paymentFailureResponseDto;
        paymentFailureResponseDto = PaymentFailureResponseDto.paymentFailureResponseWith()
            .paymentFailureInitiated(toPaymentFailureInitiated(paymentFailure))
            .paymentFailureClosed((paymentFailure.getRepresentmentSuccess() != null ? toPaymentFailureClosed(paymentFailure) : null))
            .build();

        return paymentFailureResponseDto;
    }

    private PaymentFailureInitiatedDto toPaymentFailureInitiated(PaymentFailures paymentFailure){

       return PaymentFailureInitiatedDto.paymentFailureInitiateResponseDtoWith()
               .additionalReference(paymentFailure.getAdditionalReference())
                   .failureReference(paymentFailure.getFailureReference())
               .failureType(paymentFailure.getFailureType())
               .paymentReference(paymentFailure.getPaymentReference())
               .disputedAmount(paymentFailure.getAmount())
               .failureEventDateTime(paymentFailure.getFailureEventDateTime())
               .Status("initiated")
               .hasAmountDebited(paymentFailure.getHasAmountDebited())
              .failureReason(paymentFailure.getReason())
           .build();
    }

    private PaymentFailureClosedDto toPaymentFailureClosed(PaymentFailures paymentFailure){

             return PaymentFailureClosedDto.paymentFailureClosedResponseDtoWith()
                     .additionalReference(paymentFailure.getAdditionalReference())
                     .failureType(paymentFailure.getFailureType())
                     .paymentReference(paymentFailure.getPaymentReference())
                     .failureReference(paymentFailure.getFailureReference())
                     .failureEventDateTime(paymentFailure.getFailureEventDateTime())
                     .Status("closed")
                     .representmentDate(paymentFailure.getRepresentmentOutcomeDate())
                     .representmentStatus(paymentFailure.getRepresentmentSuccess())
                     .disputedAmount(paymentFailure.getAmount())
                     .failureReason(paymentFailure.getReason())
                     .hasAmountDebited(paymentFailure.getHasAmountDebited())
                 .build();
    }

}
