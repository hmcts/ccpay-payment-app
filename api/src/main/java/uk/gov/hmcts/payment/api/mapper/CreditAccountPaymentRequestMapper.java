package uk.gov.hmcts.payment.api.mapper;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
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


        return creditAccountPaymentRequest.getFees().stream().map( feeDto -> {
            return PaymentByAccountFee.paymentByAccountFeeWith()
                .code(feeDto.getCode())
                .id(feeDto.getId())
                .version(feeDto.getVersion())
                .memosline(feeDto.getMemoLine())
                .nac(feeDto.getNaturalAccountCode())
                .jurisdiction1(feeDto.getJurisdiction1())
                .jurisdiction2(feeDto.getJurisdiction2())
                .volume(feeDto.getVolume().toString())
                .calculatedAmount(feeDto.getCalculatedAmount().toString())
                .build();
        }).toList();

        //Keeping this for now, in case it needs to be tested against API without multiple Fees
//      List<PaymentByAccountFee> paymentByAccountFee = new ArrayList<>();
//        paymentByAccountFee.add(PaymentByAccountFee.paymentByAccountFeeWith()
//            .code(creditAccountPaymentRequest.getFees().getFirst().getCode())
//            .id(creditAccountPaymentRequest.getFees().getFirst().getId())
//            .version(creditAccountPaymentRequest.getFees().getFirst().getVersion())
//            .memosline(creditAccountPaymentRequest.getFees().getFirst().getMemoLine())
//            .nac(creditAccountPaymentRequest.getFees().getFirst().getNaturalAccountCode())
//            .jurisdiction1(creditAccountPaymentRequest.getFees().getFirst().getJurisdiction1())
//            .jurisdiction2(creditAccountPaymentRequest.getFees().getFirst().getJurisdiction2())
//            .volume(creditAccountPaymentRequest.getFees().getFirst().getVolume().toString())
//            .calculatedAmount(creditAccountPaymentRequest.getFees().getFirst().getCalculatedAmount().toString())
//            .build());
//        return paymentByAccountFee;
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

}
