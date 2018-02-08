package uk.gov.hmcts.payment.api.componenttests;

import org.junit.Test;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.controllers.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class StatusMapperTest {

    @Test
    public void whenGovPayStatusIsCreated_thenShouldMappedAsInitiated() {

        CardPaymentDtoMapper mapper = new CardPaymentDtoMapper();
        CardPaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("created"));

        assertThat("Initiated", is(paymentDto.getStatus()));

    }

    @Test
    public void whenGovPayStatusIsStarted_thenShouldMappedAsInitiated() {

        CardPaymentDtoMapper mapper = new CardPaymentDtoMapper();
        CardPaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("started"));

        assertThat("Initiated", is(paymentDto.getStatus()));
    }
    @Test
    public void whenGovPayStatusIsError_thenShouldMappedAsInitiated() {

        CardPaymentDtoMapper mapper = new CardPaymentDtoMapper();
        CardPaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("error"));

        assertThat("Failed", is(paymentDto.getStatus()));
    }
    @Test
    public void whenGovPayStatusIsFailed_thenShouldMappedAsInitiated() {

        CardPaymentDtoMapper mapper = new CardPaymentDtoMapper();
        CardPaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("failed"));

        assertThat("Failed", is(paymentDto.getStatus()));
    }

    @Test
    public void whenGovPayStatusIsUnknown_thenShouldReturnSameStatus() {

        CardPaymentDtoMapper mapper = new CardPaymentDtoMapper();
        CardPaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("PBAStatus"));

        assertThat("PBAStatus", is(paymentDto.getStatus()));
    }
    private PaymentFeeLink getPaymentWithStatus(String status) {
        List payments = new ArrayList<>();
        Payment payment = Payment.paymentWith().
            paymentStatus(PaymentStatus.paymentStatusWith().name(status).build()).build();

        payments.add(payment);


        return PaymentFeeLink.paymentFeeLinkWith().payments(payments
        ).build();
    }
}
