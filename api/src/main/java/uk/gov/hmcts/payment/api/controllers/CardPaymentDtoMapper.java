package uk.gov.hmcts.payment.api.controllers;

import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.controllers.PaymentController;

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
            .status(getMappedStatus(payment.getPaymentStatus().getName()))
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .links(new CardPaymentDto.LinksDto(
                payment.getNextUrl() == null ? null : new CardPaymentDto.LinkDto(payment.getNextUrl(), "GET"),
                payment.getCancelUrl() == null ? null : cancellationLink(payment.getUserId(), payment.getId())
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
            .currency(payment.getCurrency())
            .status(payment.getStatus())
            .dateCreated(payment.getDateCreated())
            .method(payment.getPaymentMethod().getName())
            .provider(payment.getPaymentProvider().getName())
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
        return Fee.feeWith().amount(feeDto.getAmount()).code(feeDto.getCode()).version(feeDto.getVersion()).build();
    }

    private FeeDto toFeeDto(Fee fee) {
        return FeeDto.feeDtoWith().amount(fee.getAmount()).code(fee.getCode()).version(fee.getVersion()).build();
    }


    @SneakyThrows(NoSuchMethodException.class)
    private CardPaymentDto.LinkDto cancellationLink(String userId, Integer paymentId) {
        Method method = PaymentController.class.getMethod("cancel", String.class, Integer.class);
        return new CardPaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, userId, paymentId).toString(), "POST");
    }

    private String getMappedStatus(String status) {

        try {
            return GovPayStatusToPayHubStatus.valueOf(status).getMapedStatus();
        }catch (IllegalArgumentException ex){
            return status;
        }
    }
}
