package uk.gov.justice.payment.api.controllers;

import lombok.SneakyThrows;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.gov.justice.payment.api.contract.PaymentDto;
import uk.gov.justice.payment.api.model.Payment;

import java.lang.reflect.Method;

@Component
public class PaymentDtoFactory {

    public PaymentDto toDto(Payment payment) {
        return PaymentDto.paymentDtoWith()
                .paymentId(payment.getGovPayId())
                .amount(payment.getAmount())
                .state(toStateDto(payment.getStatus(), payment.getFinished()))
                .description(payment.getDescription())
                .applicationReference(payment.getApplicationReference())
                .paymentReference(payment.getPaymentReference())
                .dateCreated(payment.getDateCreated())
                .links(new PaymentDto.LinksDto(
                        payment.getNextUrl() == null ? null : new PaymentDto.LinkDto(payment.getNextUrl(), "GET"),
                        payment.getCancelUrl() == null ? null : cancellationLink(payment.getGovPayId())
                ))
                .build();
    }

    private PaymentDto.StateDto toStateDto(String status, Boolean finished) {
        return new PaymentDto.StateDto(status, finished);
    }

    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto cancellationLink(String paymentId) {
        Method method = PaymentController.class.getMethod("cancel", String.class, String.class);
        return new PaymentDto.LinkDto(ControllerLinkBuilder.linkTo(method, paymentId).toString(), "POST");
    }

}
