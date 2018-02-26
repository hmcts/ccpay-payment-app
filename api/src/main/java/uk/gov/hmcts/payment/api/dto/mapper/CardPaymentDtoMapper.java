package uk.gov.hmcts.payment.api.dto.mapper;

import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.controllers.CardPaymentController;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CardPaymentDtoMapper {

    public PaymentDto toCardPaymentDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return PaymentDto.payment2DtoWith()
            .status(PayStatusToPayHubStatus.valueOf(payment.getStatus()).mapedStatus)
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .links(new PaymentDto.LinksDto(
                payment.getNextUrl() == null ? null : new PaymentDto.LinkDto(payment.getNextUrl(), "GET"),
                null, null
            ))
            .build();
    }

    public PaymentDto toRetrieveCardPaymentResponseDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        List<Fee> fees = paymentFeeLink.getFees();
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
            .externalProvider(payment.getPaymentProvider().getName())
            .fees(toFeeDtos(fees))
            .links(new PaymentDto.LinksDto(null,
                retrieveCardPaymentLink(payment.getReference()),
                null
            ))
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
            .channel(payment.getPaymentChannel().getName())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).mapedStatus)
            .dateCreated(payment.getDateCreated())
            .dateUpdated(payment.getDateUpdated())
            .method(payment.getPaymentMethod().getName())
            .fees(toFeeDtos(paymentFeeLink.getFees()))
            .build();

    }

    public List<FeeDto> toFeeDtos(List<Fee> fees) {
        return fees.stream().map(this::toFeeDto).collect(Collectors.toList());
    }

    public List<Fee> toFees(List<FeeDto> feeDtos) {
        return feeDtos.stream().map(this::toFee).collect(Collectors.toList());
    }

    private Fee toFee(FeeDto feeDto) {
        return Fee.feeWith().calculatedAmount(feeDto.getCalculatedAmount()).code(feeDto.getCode()).version(feeDto.getVersion()).build();
    }

    private FeeDto toFeeDto(Fee fee) {
        return FeeDto.feeDtoWith().calculatedAmount(fee.getCalculatedAmount()).code(fee.getCode()).version(fee.getVersion()).build();
    }


    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto cancellationLink(String userId, Integer paymentId) {
        Method method = CardPaymentController.class.getMethod("cancel", String.class, Integer.class);
        return new PaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, userId, paymentId).toString(), "POST");
    }

    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto retrieveCardPaymentLink(String reference) {
        Method method = CardPaymentController.class.getMethod("retrieve", String.class);
        return new PaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, reference).toString(), "GET");
    }

}
