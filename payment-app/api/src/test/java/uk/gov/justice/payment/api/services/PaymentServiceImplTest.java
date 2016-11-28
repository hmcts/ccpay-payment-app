package uk.gov.justice.payment.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.GovPayment;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.State;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import static org.mockito.Mockito.*;
import static uk.gov.justice.payment.api.external.client.dto.GovPayment.govPaymentWith;
import static uk.gov.justice.payment.api.model.Payment.paymentWith;

public class PaymentServiceImplTest {

    private PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private GovPayConfig govPayConfig = mock(GovPayConfig.class);
    private GovPayClient govPayClient = mock(GovPayClient.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void createCallsGovPayAndSavesTheResponse() throws JsonProcessingException {
        GovPayment govPayResponse = govPaymentWith()
                .paymentId("paymentId")
                .state(new State("status", false, "any", "any"))
                .createdDate("createdDate")
                .links(new GovPayment.Links(
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
                paymentWith()
                        .serviceId("divorce")
                        .govPayId("paymentId")
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
                        .build());
    }

    @Test
    public void findByPaymentIdCallsGovPayAndUpdatesPayment() throws JsonProcessingException {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(govPayClient.getPayment("authorizationKey", "paymentId")).thenReturn(govPaymentWith()
                .state(new State("newStatus", true, "any", "any"))
                .links(new GovPayment.Links(
                        null,
                        new Link("any", ImmutableMap.of(), "newNextUrl", "any"),
                        null,
                        null,
                        null,
                        new Link("any", ImmutableMap.of(), "newCancelUrl", "any")
                ))
                .build()
        );

        when(paymentRepository.findByGovPayId("paymentId")).thenReturn(paymentWith()
                .status("oldStatus")
                .finished(false)
                .nextUrl("oldNextUrl")
                .cancelUrl("oldCancelUrl")
                .build());

        paymentService().findByPaymentId("divorce", "paymentId");

        verify(paymentRepository).save(
                paymentWith()
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