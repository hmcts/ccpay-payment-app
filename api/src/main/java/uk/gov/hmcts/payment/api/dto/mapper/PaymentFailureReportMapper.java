package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.PaymentFailureReportDto;
import uk.gov.hmcts.payment.api.dto.RefundDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFailures;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentFailureReportMapper {

    public PaymentFailureReportDto failureReportMapper(PaymentFailures paymentFailures, Payment payment, List<RefundDto> refund) {

        PaymentFailureReportDto paymentFailureReportDto = PaymentFailureReportDto.paymentFailureReportDtoWith()
            .ccdReference(null != payment ? payment.getCcdCaseNumber():null)
            .disputedAmount(paymentFailures.getAmount())
            .failureReason(paymentFailures.getReason())
            .failureReference(paymentFailures.getFailureReference())
            .eventDate(paymentFailures.getFailureEventDateTime())
            .eventName(paymentFailures.getFailureType())
            .paymentReference(null != payment ? payment.getReference():null)
            .orgId(null != payment ? payment.getPaymentLink().getOrgId():null)
            .refundAmount(null != refund ? toRefundAmount(refund,paymentFailures) :null)
            .refundDate(null != refund ? toRefundDate(refund,paymentFailures) :null)
            .refundReference(null != refund ? toRefundReference(refund,paymentFailures) :null)
            .representmentDate(paymentFailures.getRepresentmentOutcomeDate())
            .representmentStatus(paymentFailures.getRepresentmentSuccess())
            .serviceName(null != payment ? payment.getServiceType():null)
            .build();
        return paymentFailureReportDto;
    }

    private String toRefundReference(List<RefundDto> refund,PaymentFailures paymentFailure ) {
        List<RefundDto> refundRes;
        String refundRef = null;
         if(!refund.isEmpty()) {
             refundRes = refund.stream()
                 .filter(dto -> paymentFailure.getPaymentReference().equals(dto.getPaymentReference()))
                 .collect(Collectors.toList());

             refundRef = refundRes
                 .stream()
                 .map(a -> String.valueOf(a.getRefundReference()))
                 .collect(Collectors.joining(","));
         }if(refundRef.equalsIgnoreCase("")){
             return null;
        }
        return refundRef;
    }

    private String toRefundDate(List<RefundDto> refund,PaymentFailures paymentFailure) {

        List<RefundDto> refundRes;
        String refundDate =null;
        if(!refund.isEmpty()) {
            refundRes = refund.stream()
                .filter(dto -> paymentFailure.getPaymentReference().equals(dto.getPaymentReference()))
                .collect(Collectors.toList());

            refundDate = refundRes
                .stream()
                .map(a -> String.valueOf(a.getRefundDate()))
                .collect(Collectors.joining(","));
        }if(refundDate.equalsIgnoreCase("")){
            return null;
        }
        return refundDate;
    }

    private String  toRefundAmount(List<RefundDto> refund,PaymentFailures paymentFailure) {
        List<RefundDto> refundRes;
        String refundAmount = null;
        if(!refund.isEmpty()) {
            refundRes = refund.stream()
                .filter(dto -> paymentFailure.getPaymentReference().equals(dto.getPaymentReference()))
                .collect(Collectors.toList());
            refundAmount = refundRes
                .stream()
                .map(a -> String.format("%.2f", a.getAmount()))
                .collect(Collectors.joining(","));
        }if(refundAmount.equalsIgnoreCase("")){
            return null;
        }
        return refundAmount;

    }
}
