package uk.gov.hmcts.payment.api.mapper;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.PaymentByAccountFee;
import uk.gov.hmcts.payment.api.dto.PaymentByAccountPayment;
import uk.gov.hmcts.payment.api.dto.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreditAccountPaymentRequestMapper {

    private final static String PAYMENT_CHANNEL_ONLINE = "online";

    @Autowired
    private CreditAccountDtoMapper creditAccountDtoMapper;

    public Payment mapPBARequest(CreditAccountPaymentRequest creditAccountPaymentRequest)
    {
        return Payment.paymentWith()
            .amount(creditAccountPaymentRequest.getAmount())
            .description(creditAccountPaymentRequest.getDescription())
            .ccdCaseNumber(creditAccountPaymentRequest.getCcdCaseNumber())
            .caseReference(creditAccountPaymentRequest.getCaseReference())
            .currency(creditAccountPaymentRequest.getCurrency().getCode())
            .serviceType(creditAccountPaymentRequest.getService())
            .customerReference(creditAccountPaymentRequest.getCustomerReference())
            .organisationName(creditAccountPaymentRequest.getOrganisationName())
            .pbaNumber(creditAccountPaymentRequest.getAccountNumber())
            .siteId(creditAccountPaymentRequest.getSiteId())
            .paymentChannel(PaymentChannel.paymentChannelWith().name(PAYMENT_CHANNEL_ONLINE).build())
            .build();
    }

    public List<PaymentFee> mapPBAFeesFromRequest(CreditAccountPaymentRequest creditAccountPaymentRequest)
    {
        List<PaymentFee> fees = creditAccountPaymentRequest.getFees().stream()
            .map(f -> creditAccountDtoMapper.toFee(f))
            .collect(Collectors.toList());

        fees.stream().forEach(fee -> {
            fee.setCcdCaseNumber((fee.getCcdCaseNumber() == null || fee.getCcdCaseNumber().isEmpty())
                ? creditAccountPaymentRequest.getCcdCaseNumber()
                : fee.getCcdCaseNumber());
        });
        return fees;
    }

    @Valid
    public PaymentByAccountRequest mapPaymentByAccountRequest(CreditAccountPaymentRequest creditAccountPaymentRequest)
    {
        PaymentByAccountPayment paymentByAccountPayment = mapPaymentAccountPayment(creditAccountPaymentRequest);
        return PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber(creditAccountPaymentRequest.getAccountNumber())
            .payment(paymentByAccountPayment)
            .build();
    }

    private List<PaymentByAccountFee> mapPaymentAccountFee(CreditAccountPaymentRequest creditAccountPaymentRequest) {

        return creditAccountPaymentRequest.getFees().stream().map( feeDto ->
            PaymentByAccountFee.paymentByAccountFeeWith()
            .code(feeDto.getCode())
            .id(feeDto.getId())
            .version(feeDto.getVersion())
            .memosline(feeDto.getMemoLine())
            .nac(feeDto.getNaturalAccountCode())
            .jurisdiction1(feeDto.getJurisdiction1())
            .jurisdiction2(feeDto.getJurisdiction2())
            .volume(toStringOrNull(feeDto.getVolume()))
            .calculatedAmount(toStringOrNull(feeDto.getCalculatedAmount()))
            .build()).toList();
    }


    private PaymentByAccountPayment mapPaymentAccountPayment(CreditAccountPaymentRequest creditAccountPaymentRequest) {

        //NB: groupReference, paymentReference, dateCreated and surname are not present/mapped in CreditAccountPaymentRequest.
        return PaymentByAccountPayment.paymentByAccountPaymentWith()
            .serviceName(creditAccountPaymentRequest.getService())
            .amount(creditAccountPaymentRequest.getAmount().toString())
            .currency(creditAccountPaymentRequest.getCurrency().getCode())
            .siteId(creditAccountPaymentRequest.getSiteId())
            .caseReference(creditAccountPaymentRequest.getCaseReference())
            .ccdCaseNumber(creditAccountPaymentRequest.getCcdCaseNumber())
            .customerReference(creditAccountPaymentRequest.getCustomerReference())
            .paymentByAccountFees(mapPaymentAccountFee(creditAccountPaymentRequest))
            .build();
    }


    public PaymentByAccountRequest mapPaymentByAccountRequest(PaymentDto paymentDto)
    {
        PaymentByAccountPayment paymentByAccountPayment = mapPaymentAccountPayment(paymentDto);
        return PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber(paymentDto.getAccountNumber())
            .payment(paymentByAccountPayment)
            .build();
    }




    private PaymentByAccountPayment mapPaymentAccountPayment(PaymentDto paymentDto) {

        //Note: surname is not mapped
        return PaymentByAccountPayment.paymentByAccountPaymentWith()
            .serviceName(paymentDto.getServiceName())
            .groupReference(paymentDto.getReference())
            .paymentReference(paymentDto.getPaymentReference())
            .dateCreated(paymentDto.getDateCreated().toString())
            .amount(paymentDto.getAmount().toString())
            .currency(paymentDto.getCurrency().getCode())
            .siteId(paymentDto.getSiteId())
            .caseReference(paymentDto.getCaseReference())
            .ccdCaseNumber(paymentDto.getCcdCaseNumber())
            .customerReference(paymentDto.getCustomerReference())
            .paymentByAccountFees(mapPaymentAccountFee(paymentDto))
            .build();
    }


    private List<PaymentByAccountFee> mapPaymentAccountFee(PaymentDto paymentDto) {

        return paymentDto.getFees().stream().map(
            fee -> PaymentByAccountFee.paymentByAccountFeeWith()
            .code(fee.getCode())
            .id(fee.getId())
            .version(fee.getVersion())
            .memosline(fee.getMemoLine())
            .nac(fee.getNaturalAccountCode())
            .jurisdiction1(fee.getJurisdiction1())
            .jurisdiction2(fee.getJurisdiction2())
            .volume(toStringOrNull(fee.getVolume()))
            .calculatedAmount(toStringOrNull(fee.getCalculatedAmount()))
            .build()).toList();
        }

    private String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }

}
