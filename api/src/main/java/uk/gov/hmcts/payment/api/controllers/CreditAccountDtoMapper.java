package uk.gov.hmcts.payment.api.controllers;

import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentGroupDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreditAccountDtoMapper {

    public PaymentDto toCreateCreditAccountPaymentResponse(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return PaymentDto.payment2DtoWith()
            .status(payment.getPaymentStatus().getName())
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .build();
    }

    public Payment toPaymentRequest(CreditAccountPaymentRequest creditAccountPayment) {
        return Payment.paymentWith()
            .amount(creditAccountPayment.getAmount())
            .description(creditAccountPayment.getDescription())
            .ccdCaseNumber(creditAccountPayment.getCcdCaseNumber())
            .caseReference(creditAccountPayment.getCaseReference())
            .serviceType(creditAccountPayment.getService().getName())
            .currency(creditAccountPayment.getCurrency().getCode())
            .customerReference(creditAccountPayment.getCustomerReference())
            .ccdCaseNumber(creditAccountPayment.getCcdCaseNumber())
            .pbaNumber(creditAccountPayment.getAccountNumber())
            .siteId(creditAccountPayment.getSiteId())
            .build();
    }

    public PaymentGroupDto toRetrievePaymentGroupReferenceResponse(PaymentFeeLink paymentFeeLink) {
        return PaymentGroupDto.paymentGroupWith()
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .payments(paymentFeeLink.getPayments().stream().map(this::toPaymentDto).collect(Collectors.toList()))
            .fees(paymentFeeLink.getFees().stream().map(this::toFeeDto).collect(Collectors.toList()))
            .build();
    }

    public PaymentDto toRetrievePaymentResponse(Payment payment, List<Fee> fees) {
        return PaymentDto.payment2DtoWith()
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .amount(payment.getAmount())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .status(payment.getPaymentStatus().getName())
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .description(payment.getDescription())
            .channel(payment.getPaymentChannel().getName())
            .method(payment.getPaymentMethod().getName())
            .externalReference(payment.getExternalReference())
            .customerReference(payment.getCustomerReference())
            .organisationName(payment.getOrganisationName())
            .accountNumber(payment.getPbaNumber())
            .fees(toFeeDtos(fees))
            .links(new PaymentDto.LinksDto(null,
                retrievePaymentLink(payment.getReference()),
                null
            ))
            .build();
    }

    public Payment toPayment(PaymentDto paymentDto) {
        return Payment.paymentWith()
            .amount(paymentDto.getAmount())
            .description(paymentDto.getDescription())
            .ccdCaseNumber(paymentDto.getCcdCaseNumber())
            .caseReference(paymentDto.getCaseReference())
            .serviceType(paymentDto.getServiceName())
            .currency(paymentDto.getCurrency().getCode())
            .customerReference(paymentDto.getCustomerReference())
            .ccdCaseNumber(paymentDto.getCcdCaseNumber())
            .pbaNumber(paymentDto.getAccountNumber())
            .siteId(paymentDto.getSiteId())
            .build();
    }


    public PaymentDto toPaymentDto(Payment payment) {
        return PaymentDto.payment2DtoWith()
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .amount(payment.getAmount())
            .description(payment.getDescription())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .caseReference(payment.getCaseReference())
            .serviceName(payment.getServiceType())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .customerReference(payment.getCustomerReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .accountNumber(payment.getPbaNumber())
            .siteId(payment.getSiteId())
            .build();
    }
    public PaymentDto toReconciliationResponseDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return PaymentDto.payment2DtoWith()
            .paymentReference(payment.getReference())
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .amount(payment.getAmount())
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .accountNumber(payment.getPbaNumber())
            .organisationName(payment.getOrganisationName())
            .customerReference(payment.getCustomerReference())
            .channel(payment.getPaymentChannel().getName())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .status(payment.getPaymentStatus().getName())
            .dateCreated(payment.getDateCreated())
            .dateUpdated(payment.getDateUpdated())
            .method(payment.getPaymentMethod().getName())
            .fees(toFeeDtos(paymentFeeLink.getFees()))
            .build();

    }

    public List<Payment> toPaymentsRequest(List<CreditAccountPaymentRequest> creditAccountPayments) {
        return creditAccountPayments.stream().map(this::toPaymentRequest).collect(Collectors.toList());
    }


    public List<FeeDto> toFeeDtos(List<Fee> fees) {
        return fees.stream().map(this::toFeeDto).collect(Collectors.toList());
    }

    public List<Fee> toFees(List<FeeDto> feeDtos) {
        return feeDtos.stream().map(this::toFee).collect(Collectors.toList());
    }

    public Fee toFee(FeeDto feeDto) {
        return Fee.feeWith().calculatedAmount(feeDto.getCalculatedAmount()).code(feeDto.getCode()).version(feeDto.getVersion()).build();
    }

    public FeeDto toFeeDto(Fee fee) {
        return FeeDto.feeDtoWith().calculatedAmount(fee.getCalculatedAmount()).code(fee.getCode()).version(fee.getVersion()).build();
    }


    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto cancellationLink(String userId, Integer paymentId) {
        Method method = CreditAccountPaymentController.class.getMethod("cancel", String.class, Integer.class);
        return new PaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, userId, paymentId).toString(), "POST");
    }

    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto retrievePaymentLink(String reference) {
        Method method = CreditAccountPaymentController.class.getMethod("retrieve", String.class);
        return new PaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, reference).toString(), "GET");
    }
}
