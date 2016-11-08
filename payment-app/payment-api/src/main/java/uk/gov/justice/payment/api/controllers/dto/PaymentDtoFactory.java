package uk.gov.justice.payment.api.controllers.dto;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import uk.gov.justice.payment.api.controllers.PaymentController;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.Payment;
import uk.gov.justice.payment.api.external.client.dto.State;

import java.lang.reflect.Method;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Component
public class PaymentDtoFactory {

    public PaymentDto toDto(Payment payment) {
        return PaymentDto.paymentDtoWith()
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .state(toStateDto(payment.getState()))
                .description(payment.getDescription())
                .reference(payment.getReference())
                .createdDate(payment.getCreatedDate())
                .links(new PaymentDto.LinksDto(
                        toLinkDto(payment.getLinks().getNextUrl()),
                        cancellationLink(payment)
                ))
                .build();
    }

    private PaymentDto.StateDto toStateDto(State state) {
        return new PaymentDto.StateDto(state.getStatus(), state.getFinished(), state.getMessage(), state.getCode());
    }

    private PaymentDto.LinkDto toLinkDto(Link nextUrl) {
        return new PaymentDto.LinkDto(nextUrl.getHref(), nextUrl.getMethod());
    }

    @SneakyThrows(NoSuchMethodException.class)
    private PaymentDto.LinkDto cancellationLink(Payment payment) {
        Method method = PaymentController.class.getMethod("cancelPayment", String.class, String.class);
        return new PaymentDto.LinkDto(linkTo(method, payment.getPaymentId()).toString(), "POST");
    }

}
