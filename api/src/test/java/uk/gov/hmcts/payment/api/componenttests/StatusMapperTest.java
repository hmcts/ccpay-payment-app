package uk.gov.hmcts.payment.api.componenttests;

import org.junit.Test;
import uk.gov.hmcts.payment.api.dto.CreateCardPaymentResponse;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class StatusMapperTest {

    @Test
    public void whenGovPayStatusIsCreated_thenShouldMappedAsInitiated() {

        PaymentDtoMapper mapper = new PaymentDtoMapper();
        CreateCardPaymentResponse
            payment = mapper.toCardPaymentDto(getPaymentWithStatus("created"));

        assertThat("Initiated", is(payment.getStatus()));

    }

    @Test
    public void whenGovPayStatusIsStarted_thenShouldMappedAsInitiated() {

        PaymentDtoMapper mapper = new PaymentDtoMapper();
        CreateCardPaymentResponse
            payment = mapper.toCardPaymentDto(getPaymentWithStatus("started"));

        assertThat("Initiated", is(payment.getStatus()));
    }
    @Test
    public void whenGovPayStatusIsError_thenShouldMappedAsInitiated() {

        PaymentDtoMapper mapper = new PaymentDtoMapper();
        CreateCardPaymentResponse payment = mapper.toCardPaymentDto(getPaymentWithStatus("error"));

        assertThat("Failed", is(payment.getStatus()));
    }
    @Test
    public void whenGovPayStatusIsFailed_thenShouldMappedAsInitiated() {

        PaymentDtoMapper mapper = new PaymentDtoMapper();
        CreateCardPaymentResponse payment = mapper.toCardPaymentDto(getPaymentWithStatus("failed"));

        assertThat("Failed", is(payment.getStatus()));
    }

    public void whenGovPayStatusIsUnknown_thenShouldReturnSameStatus() {

        PaymentDtoMapper mapper = new PaymentDtoMapper();
        CreateCardPaymentResponse payment = mapper.toCardPaymentDto(getPaymentWithStatus("PBAStatus"));

        assertThat("PBAStatus", is(payment.getStatus()));
    }

    private PaymentFeeLink getPaymentWithStatus(String status) {
        List payments = new ArrayList<>();
        Payment payment = Payment.paymentWith().
            status(status).build();

        payments.add(payment);


        return PaymentFeeLink.paymentFeeLinkWith().payments(payments).build();
    }
}
