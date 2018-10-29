package uk.gov.hmcts.payment.api.dto.mapper;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.StatusHistoryDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.controllers.CardPaymentController;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PaymentDtoMapper {

    @Autowired
    private FeesService feesService;

    public PaymentDto toCardPaymentDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return PaymentDto.payment2DtoWith()
            .status(PayStatusToPayHubStatus.valueOf(payment.getStatus().toLowerCase()).mapedStatus)
            .reference(payment.getReference())
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .dateCreated(payment.getDateCreated())
            .links(new PaymentDto.LinksDto(
                payment.getNextUrl() == null ? null : new PaymentDto.LinkDto(payment.getNextUrl(), "GET"),
                null, null
            ))
            .build();
    }

    public PaymentDto toRetrieveCardPaymentResponseDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        List<PaymentFee> fees = paymentFeeLink.getFees();
        return PaymentDto.payment2DtoWith()
            .reference(payment.getReference())
            .amount(payment.getAmount())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).mapedStatus)
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .description(payment.getDescription())
            .channel(payment.getPaymentChannel().getName())
            .method(payment.getPaymentMethod().getName())
            .externalReference(payment.getExternalReference())
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .externalProvider(payment.getPaymentProvider() != null ? payment.getPaymentProvider().getName() : null)
            .fees(toFeeDtos(fees))
            .links(new PaymentDto.LinksDto(null,
                retrieveCardPaymentLink(payment.getReference()),
                null
            ))
            .build();
    }

    public PaymentDto toRetrievePaymentStatusesDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return PaymentDto.payment2DtoWith()
            .reference(payment.getReference())
            .amount(payment.getAmount())
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).mapedStatus)
            .statusHistories(toStatusHistoryDtos(payment.getStatusHistories()))
            .build();
    }

    public PaymentDto toReconciliationResponseDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        PaymentDto paymentDto = PaymentDto.payment2DtoWith()
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
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).mapedStatus)
            .dateCreated(payment.getDateCreated())
            .dateUpdated(payment.getDateUpdated())
            .method(payment.getPaymentMethod().getName())
            .giroSlipNo(payment.getGiroSlipNo())
            .externalProvider(payment.getPaymentProvider() != null ? payment.getPaymentProvider().getName() : null)
            .externalReference(payment.getExternalReference())
            .reportedDateOffline(payment.getReportedDateOffline() != null ? payment.getReportedDateOffline().toString() : null)
            .fees(toFeeDtos(paymentFeeLink.getFees()))
            .build();
        return enrichWithFeeData(paymentDto);
    }

    private PaymentDto enrichWithFeeData(PaymentDto paymentDto) {
        paymentDto.getFees().forEach(fee -> {
            Optional<FeeVersionDto> optionalFeeVersionDto = feesService.getFeeVersion(fee.getCode(), fee.getVersion());
            if (optionalFeeVersionDto.isPresent()) {
                fee.setMemoLine(optionalFeeVersionDto.get().getMemoLine());
                fee.setNaturalAccountCode(optionalFeeVersionDto.get().getNaturalAccountCode());
            }
        });
        return paymentDto;
    }

    private List<FeeDto> toFeeDtos(List<PaymentFee> fees) {
        return fees.stream().map(this::toFeeDto).collect(Collectors.toList());
    }

    public List<PaymentFee> toFees(List<FeeDto> feeDtos) {
        return feeDtos.stream().map(this::toFee).collect(Collectors.toList());
    }

    private PaymentFee toFee(FeeDto feeDto) {
        return PaymentFee.feeWith()
            .calculatedAmount(feeDto.getCalculatedAmount())
            .code(feeDto.getCode())
            .version(feeDto.getVersion())
            .volume(feeDto.getVolume() == null ? 1 : feeDto.getVolume().intValue())
            .build();
    }

    private FeeDto toFeeDto(PaymentFee fee) {
        return FeeDto.feeDtoWith()
            .calculatedAmount(fee.getCalculatedAmount())
            .code(fee.getCode())
            .version(fee.getVersion())
            .volume(fee.getVolume())
            .ccdCaseNumber(fee.getCcdCaseNumber() != null ? fee.getCcdCaseNumber(): null)
            .reference(fee.getReference() != null ? fee.getReference(): null)
            .build();

    }

    private List<StatusHistoryDto> toStatusHistoryDtos(List<StatusHistory> statusHistories) {
        return statusHistories.stream().map(this::toStatusHistoryDto).collect(Collectors.toList());
    }

    private StatusHistoryDto toStatusHistoryDto(StatusHistory statusHistory) {
        return StatusHistoryDto.statusHistoryDtoWith()
            .status(statusHistory.getStatus())
            .externalStatus(statusHistory.getExternalStatus())
            .dateCreated(statusHistory.getDateCreated())
            .build();
    }


    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto retrieveCardPaymentLink(String reference) {
        Method method = CardPaymentController.class.getMethod("retrieve", String.class);
        return new PaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, reference).toString(), "GET");
    }

}
