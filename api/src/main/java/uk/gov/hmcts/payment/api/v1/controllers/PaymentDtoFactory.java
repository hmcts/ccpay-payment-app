package uk.gov.hmcts.payment.api.v1.controllers;

import java.lang.reflect.Method;
import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.contract.PaymentDto;
import uk.gov.hmcts.payment.api.v1.model.PaymentOld;

@Component
public class PaymentDtoFactory {

    public PaymentDto toDto(PaymentOld paymentOld) {
        return PaymentDto.paymentDtoWith()
                .id(paymentOld.getId().toString())
                .amount(paymentOld.getAmount())
                .state(toStateDto(paymentOld.getStatus(), paymentOld.getFinished()))
                .description(paymentOld.getDescription())
                .reference(paymentOld.getReference())
                .dateCreated(paymentOld.getDateCreated())
                .links(new PaymentDto.LinksDto(
                        paymentOld.getNextUrl() == null ? null : new PaymentDto.LinkDto(paymentOld.getNextUrl(), "GET"),
                        paymentOld.getCancelUrl() == null ? null : cancellationLink(paymentOld.getUserId(), paymentOld.getId())
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
