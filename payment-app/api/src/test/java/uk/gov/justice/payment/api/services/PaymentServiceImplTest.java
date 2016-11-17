package uk.gov.justice.payment.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.Payment;
import uk.gov.justice.payment.api.external.client.dto.State;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import static org.mockito.Mockito.*;
import static uk.gov.justice.payment.api.external.client.dto.Payment.paymentWith;
import static uk.gov.justice.payment.api.model.PaymentDetails.paymentDetailsWith;

public class PaymentServiceImplTest {

    private PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private GovPayConfig govPayConfig = mock(GovPayConfig.class);
    private GovPayClient govPayClient = mock(GovPayClient.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void createCallsGovPayAndSavesTheResponse() throws JsonProcessingException {
        Payment govPayResponse = paymentWith()
                .paymentId("paymentId")
                .state(new State("status", false, "any", "any"))
                .createdDate("createdDate")
                .links(new Payment.Links(
                        null,
                        new Link("any", ImmutableMap.of(), "nextUrl", "any"),
                        null,
                        null,
                        null,
                        new Link("any", ImmutableMap.of(), "cancelUrl", "any")
                ))
                .build();

        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(govPayClient.createPayment("authorizationKey", new CreatePaymentRequest(100, "applicationReference", "description", "returnUrl"))).thenReturn(govPayResponse);

        paymentService().create("divorce", 100, "email", "applicationReference", "paymentReference", "description", "returnUrl");

        verify(paymentRepository).save(
                paymentDetailsWith()
                        .serviceId("divorce")
                        .paymentId("paymentId")
                        .amount(100)
                        .email("email")
                        .status("status")
                        .finished(false)
                        .applicationReference("applicationReference")
                        .paymentReference("paymentReference")
                        .description("description")
                        .returnUrl("returnUrl")
                        .nextUrl("nextUrl")
                        .cancelUrl("cancelUrl")
                        .response(objectMapper.writeValueAsString(govPayResponse))
                        .createdDate("createdDate")
                        .build());
    }

    @Test
    public void findByPaymentIdCallsGovPayAndUpdatesPayment() throws JsonProcessingException {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(govPayClient.getPayment("authorizationKey", "paymentId")).thenReturn(paymentWith()
                .state(new State("newStatus", true, "any", "any"))
                .links(new Payment.Links(
                        null,
                        new Link("any", ImmutableMap.of(), "newNextUrl", "any"),
                        null,
                        null,
                        null,
                        new Link("any", ImmutableMap.of(), "newCancelUrl", "any")
                ))
                .build()
        );

        when(paymentRepository.findByPaymentId("paymentId")).thenReturn(paymentDetailsWith()
                .status("oldStatus")
                .finished(false)
                .nextUrl("oldNextUrl")
                .cancelUrl("oldCancelUrl")
                .build());

        paymentService().findByPaymentId("divorce", "paymentId");

        verify(paymentRepository).save(
                paymentDetailsWith()
                        .status("newStatus")
                        .finished(true)
                        .nextUrl("newNextUrl")
                        .cancelUrl("newCancelUrl")
                        .build());
    }

    private PaymentServiceImpl paymentService() {
        return new PaymentServiceImpl(objectMapper, govPayConfig, govPayClient, paymentRepository);
    }

}