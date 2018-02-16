package uk.gov.hmcts.payment.api.controllers;

import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CardPaymentDtoMapper {

    private enum GovPayStatusToPayHubStatus {
        created("Initiated"), started("Initiated"), submitted("Initiated"), success("Success"), failed("Failed"), cancelled("Failed"), error("Failed");

        private String mapedStatus;

        GovPayStatusToPayHubStatus(String status) {
            this.mapedStatus = status;
        }

        String getMapedStatus() {
            return mapedStatus;
        }

    };
    public CardPaymentDto toCardPaymentDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return CardPaymentDto.payment2DtoWith()
            .status(getMappedStatus(payment.getStatus()))
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .links(new CardPaymentDto.LinksDto(
                payment.getNextUrl() == null ? null : new CardPaymentDto.LinkDto(payment.getNextUrl(), "GET"),
                null, null
            ))
            .build();
    }

    public CardPaymentDto toRetrieveCardPaymentResponseDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return CardPaymentDto.payment2DtoWith()
            .reference(payment.getReference())
            .amount(payment.getAmount())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .status(getMappedStatus(payment.getStatus()))
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .description(payment.getDescription())
            .channel(payment.getPaymentChannel().getName())
            .method(payment.getPaymentMethod().getName())
            .externalReference(payment.getExternalReference())
            .externalProvider(payment.getPaymentProvider().getName())
            .links(new CardPaymentDto.LinksDto(null,
                retrieveCardPaymentLink(payment.getReference()),
                null
            ))
            .build();
    }

    public CardPaymentDto toReconciliationResponseDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return CardPaymentDto.payment2DtoWith()
            .paymentReference(payment.getReference())
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .amount(payment.getAmount())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .status(payment.getStatus())
            .dateCreated(payment.getDateCreated())
            .method(payment.getPaymentMethod().getName())
            .externalProvider(payment.getPaymentProvider().getName())
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
    private CardPaymentDto.LinkDto cancellationLink(String userId, Integer paymentId) {
        Method method = CardPaymentController.class.getMethod("cancel", String.class, Integer.class);
        return new CardPaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, userId, paymentId).toString(), "POST");
    }

    @SneakyThrows(NoSuchMethodException.class)
    private CardPaymentDto.LinkDto retrieveCardPaymentLink(String reference) {
        Method method = CardPaymentController.class.getMethod("retrieve", String.class);
        return new CardPaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, reference).toString(), "GET");
    }

    private String getMappedStatus(String status) {

        try {
            return GovPayStatusToPayHubStatus.valueOf(status).getMapedStatus();
        }catch (IllegalArgumentException ex){
            return status;
        }
    }
}
