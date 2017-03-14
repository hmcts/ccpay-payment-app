package uk.gov.hmcts.payment.api.controllers;

import java.lang.reflect.Method;
import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.model.Payment;

@Component
public class PaymentDtoFactory {

    public PaymentDto toDto(Payment payment) {
        return PaymentDto.paymentDtoWith()
                .id(payment.getId().toString())
                .amount(payment.getAmount())
                .state(toStateDto(payment.getStatus(), payment.getFinished()))
                .description(payment.getDescription())
                .reference(payment.getReference())
                .dateCreated(payment.getDateCreated())
                .links(new PaymentDto.LinksDto(
                        payment.getNextUrl() == null ? null : new PaymentDto.LinkDto(payment.getNextUrl(), "GET"),
                        payment.getCancelUrl() == null ? null : cancellationLink(payment.getUserId(), payment.getId())
                ))
                .build();
    }

    private PaymentDto.StateDto toStateDto(String status, Boolean finished) {
        return new PaymentDto.StateDto(status, finished);
    }

    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto cancellationLink(String userId, Integer paymentId) {
        Method method = PaymentController.class.getMethod("cancel", String.class, Integer.class);
        return new PaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, userId, paymentId).toString(), "POST");
    }

}
