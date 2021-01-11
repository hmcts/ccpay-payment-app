package uk.gov.hmcts.payment.api.dto.mapper;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.controllers.CreditAccountPaymentController;
import uk.gov.hmcts.payment.api.contract.CreditAccountFeeDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreditAccountDtoMapper {

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    public CreditAccountPaymentCreatedResponse toCreateCreditAccountPaymentResponse(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return CreditAccountPaymentCreatedResponse.creditAccountPaymentResponse()
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).getMappedStatus())
            .reference(payment.getReference())
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .dateCreated(payment.getDateCreated())
            .statusHistories(payment.getStatusHistories()
                .stream().map(this::statusHistoryToDto).collect(Collectors.toList())
            )
            .build();
    }

    private StatusHistoryDto statusHistoryToDto(StatusHistory statusHistory) {
        return StatusHistoryDto.statusHistoryDtoWith()
            .status(statusHistory.getStatus())
            .externalStatus(statusHistory.getExternalStatus())
            .errorCode(statusHistory.getErrorCode())
            .errorMessage(statusHistory.getMessage())
            .dateCreated(statusHistory.getDateCreated())
            .dateUpdated(statusHistory.getDateUpdated())
            .build();
    }

    public CreditAccountPaymentResponse toRetrievePaymentResponse(Payment payment, List<PaymentFee> fees) {
        return CreditAccountPaymentResponse.creditAccountPaymentResponseWith()
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .amount(payment.getAmount())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).getMappedStatus())
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .description(payment.getDescription())
            .channel(payment.getPaymentChannel().getName())
            .method(payment.getPaymentMethod().getName())
            .externalReference(payment.getExternalReference())
            .customerReference(payment.getCustomerReference())
            .organisationName(payment.getOrganisationName())
            .accountNumber(payment.getPbaNumber())
            .fees(toCreditAccountFeeDtos(fees))
            .links(new PaymentDto.LinksDto(null,
                retrievePaymentLink(payment.getReference()),
                null
            ))
            .build();
    }

    public CreditAccountPaymentStatusResponse toRetrievePaymentStatusResponse(Payment payment) {
        return CreditAccountPaymentStatusResponse.retrievePaymentResponseWith()
            .reference(payment.getReference())
            .amount(payment.getAmount())
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).getMappedStatus())
            .statusHistories(toStatusHistoryDtos(payment.getStatusHistories()))
            .build();
    }

    private List<StatusHistoryDto> toStatusHistoryDtos(List<StatusHistory> statusHistories) {
        return statusHistories.stream().map(this::toStatusHistoryDto).collect(Collectors.toList());
    }

    private StatusHistoryDto toStatusHistoryDto(StatusHistory statusHistory) {
        return StatusHistoryDto.statusHistoryDtoWith()
            .status(PayStatusToPayHubStatus.valueOf(statusHistory.getStatus()).getMappedStatus())
            .dateCreated(statusHistory.getDateCreated())
            .build();
    }

    public List<CreditAccountFeeDto> toCreditAccountFeeDtos(List<PaymentFee> fees) {
        return fees.stream().map(this::toCreditAccountFeeDto).collect(Collectors.toList());
    }

    public PaymentFee toFee(FeeDto feeDto) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
        return PaymentFee.feeWith()
            .calculatedAmount(feeDto.getCalculatedAmount())
            .code(feeDto.getCode())
            .version(feeDto.getVersion())
            .volume(feeDto.getVolume() == null ? 1 : feeDto.getVolume().intValue())
            .ccdCaseNumber(feeDto.getCcdCaseNumber())
            .feeAmount(feeDto.getFeeAmount())
            .netAmount(feeDto.getNetAmount())
            .dateCreated(apportionFeature ? timestamp: null)
            .build();
    }

    public CreditAccountFeeDto toCreditAccountFeeDto(PaymentFee fee) {
        return CreditAccountFeeDto.buildCreditAccountFeeDto()
            .calculatedAmount(fee.getCalculatedAmount())
            .code(fee.getCode()).version(fee.getVersion())
            .volume(fee.getVolume())
            .build();

    }


    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto cancellationLink(String userId, Integer paymentId) {
        Method method = CreditAccountPaymentController.class.getMethod("cancel", String.class, Integer.class);
        return new PaymentDto.LinkDto(WebMvcLinkBuilder.linkTo(method, userId, paymentId).toString(), "POST");
    }

    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto retrievePaymentLink(String reference) {
        Method method = CreditAccountPaymentController.class.getMethod("retrieve", String.class);
        return new PaymentDto.LinkDto(WebMvcLinkBuilder.linkTo(method, reference).toString(), "GET");
    }
}
