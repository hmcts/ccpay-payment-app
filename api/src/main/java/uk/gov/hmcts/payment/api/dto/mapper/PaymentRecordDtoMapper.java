package uk.gov.hmcts.payment.api.dto.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

@Component
public class PaymentRecordDtoMapper {

    public PaymentDto toCreateRecordPaymentResponse(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return PaymentDto.payment2DtoWith()
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).mapedStatus)
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .build();
    }

    public Payment toPaymentRequest(PaymentRecordRequest paymentRecordRequest) {
        return Payment.paymentWith()
            .amount(paymentRecordRequest.getAmount())
            .ccdCaseNumber(paymentRecordRequest.getCcdCaseNumber())
            .caseReference(paymentRecordRequest.getCaseReference())
            .serviceType(paymentRecordRequest.getService().getName())
            .currency(paymentRecordRequest.getCurrency().getCode())
            .siteId(paymentRecordRequest.getSiteId())
            .giro(paymentRecordRequest.getGiro())
            .build();
    }

    public PaymentFee toFee(FeeDto feeDto, Payment payment) {
        return PaymentFee.feeWith()
            .calculatedAmount(feeDto.getCalculatedAmount()).code(feeDto.getCode())
            .version(feeDto.getVersion())
            .volume(feeDto.getVolume() == null ? 1 : feeDto.getVolume())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .caseReference(payment.getCaseReference())
            .build();
    }
}
