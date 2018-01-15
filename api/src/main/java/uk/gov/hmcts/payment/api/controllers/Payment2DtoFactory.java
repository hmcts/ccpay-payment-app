package uk.gov.hmcts.payment.api.controllers;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.controllers.PaymentController;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.Payment2Dto;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

@Component
public class Payment2DtoFactory {

    public Payment2Dto toCardPaymentDto(PaymentFeeLink paymentFeeLink) {
        Payment payment = paymentFeeLink.getPayments().get(0);
        return Payment2Dto.payment2DtoWith()
            .id(payment.getId().toString())
            .amount(payment.getAmount())
            .state(toStateDto(payment.getStatus(), payment.getFinished()))
            .description(payment.getDescription())
            .reference(payment.getReference())
            .dateCreated(payment.getDateCreated())
            .links(new Payment2Dto.LinksDto(
                payment.getNextUrl() == null ? null : new Payment2Dto.LinkDto(payment.getNextUrl(), "GET"),
                payment.getCancelUrl() == null ? null : cancellationLink(payment.getUserId(), payment.getId())
            ))
            .build();
    }

    public List<Fee> toFees(List<FeeDto> feeDtos) {
        return feeDtos.stream().map(this::toFee).collect(Collectors.toList());
    }

    private Fee toFee(FeeDto feeDto) {
        return Fee.feeWith().code(feeDto.getCode()).version(feeDto.getVersion()).build();
    }

    private Payment2Dto.StateDto toStateDto(String status, Boolean finished) {
        return new Payment2Dto.StateDto(status, finished);
    }

    @SneakyThrows(NoSuchMethodException.class)
    private Payment2Dto.LinkDto cancellationLink(String userId, Integer paymentId) {
        Method method = PaymentController.class.getMethod("cancel", String.class, Integer.class);
        return new Payment2Dto.LinkDto(ControllerLinkBuilder.linkTo(method, userId, paymentId).toString(), "POST");
    }

}
