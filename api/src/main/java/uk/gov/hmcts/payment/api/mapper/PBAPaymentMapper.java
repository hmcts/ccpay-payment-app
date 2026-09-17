package uk.gov.hmcts.payment.api.mapper;

import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.liberata.FeeRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentRequest;
import uk.gov.hmcts.payment.api.model.Payment;

import java.util.Date;
import java.util.List;

@Component
public class PBAPaymentMapper {

    public PaymentAccountRequest mapToPaymentByAccountRequest(CreditAccountPaymentRequest creditAccountPaymentRequest, String groupReference, Payment payment) {
        val paymentDto = mapPaymentAccountPayment(creditAccountPaymentRequest, groupReference,payment);
        val  paymentByAccountRequest = PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber(creditAccountPaymentRequest.getAccountNumber())
            .payment(paymentDto)
            .build();

        return PaymentAccountRequest.paymentAccountRequestWith()
            .PaymentByAccountRequest(paymentByAccountRequest).build();
    }

    private PaymentRequest mapPaymentAccountPayment(CreditAccountPaymentRequest creditAccountPaymentRequest, String groupReference,Payment payment) {

        return PaymentRequest.paymentDtoWith()
            .groupReference(groupReference)
            .paymentReference(groupReference)
            .surname(payment.getPayerName())
            .serviceName(creditAccountPaymentRequest.getService())
            .amount(creditAccountPaymentRequest.getAmount().toString())
            .currency(creditAccountPaymentRequest.getCurrency().getCode())
            .siteId(creditAccountPaymentRequest.getSiteId())
            .caseReference(creditAccountPaymentRequest.getCaseReference())
            .ccdCaseNumber(creditAccountPaymentRequest.getCcdCaseNumber())
            .customerReference(creditAccountPaymentRequest.getCustomerReference())
            .fee(mapPaymentAccountFee(creditAccountPaymentRequest))
            .dateCreated(new Date())
            .build();
    }

    private List<FeeRequest> mapPaymentAccountFee(CreditAccountPaymentRequest creditAccountPaymentRequest) {

        return creditAccountPaymentRequest.getFees().stream().map(feeDto -> {
            return FeeRequest.feeWith()
                .code(feeDto.getCode())
                .id(145818)
                .version(feeDto.getVersion())
                .memoline("RECEIPT OF FEES - Civil issue possession")
                .nac("4481102134")
                .jurisdiction1("civil")
                .jurisdiction2("county court")
                .volume(feeDto.getVolume().toString())
                .calculatedAmount(feeDto.getCalculatedAmount().toString())
                .build();
        }).toList();


    }
}
