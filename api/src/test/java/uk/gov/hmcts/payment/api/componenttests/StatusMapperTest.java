package uk.gov.hmcts.payment.api.componenttests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StatusMapperTest {

    private PaymentDtoMapper mapper;

    @Before
    public void setup() {
        mapper = new PaymentDtoMapper();
    }

    @After
    public void tearDown() {
        mapper = null;
    }

    @Test
    public void whenGovPayStatusIsCreated_thenShouldMappedAsInitiated() {

        PaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("created"));

        assertEquals("Initiated", paymentDto.getStatus());

    }

    @Test
    public void whenGovPayStatusIsStarted_thenShouldMappedAsInitiated() {

        PaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("started"));

        assertEquals("Initiated", paymentDto.getStatus());
    }

    @Test
    public void whenGovPayStatusIsError_thenShouldMappedAsInitiated() {

        PaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("error"));

        assertEquals("Failed", paymentDto.getStatus());
    }

    @Test
    public void whenGovPayStatusIsFailed_thenShouldMappedAsInitiated() {

        PaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("failed"));

        assertEquals("Failed", paymentDto.getStatus());
    }

    public void whenGovPayStatusIsUnknown_thenShouldReturnSameStatus() {

        PaymentDto paymentDto = mapper.toCardPaymentDto(getPaymentWithStatus("PBAStatus"));

        assertEquals("PBAStatus", paymentDto.getStatus());
    }

    private PaymentFeeLink getPaymentWithStatus(String status) {
        List payments = new ArrayList<>();
        Payment payment = Payment.paymentWith().
            status(status).build();

        payments.add(payment);

        return PaymentFeeLink.paymentFeeLinkWith().payments(payments).build();
    }
}
