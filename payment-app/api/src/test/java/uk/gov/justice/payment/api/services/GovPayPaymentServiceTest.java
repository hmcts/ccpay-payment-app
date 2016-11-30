package uk.gov.justice.payment.api.services;

import org.junit.Test;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static uk.gov.justice.payment.api.model.Payment.paymentWith;


public class GovPayPaymentServiceTest {

    private PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private GovPayConfig govPayConfig = mock(GovPayConfig.class);
    private GovPayClient govPayClient = mock(GovPayClient.class);
    private GovPayPaymentService govPayPaymentService = new GovPayPaymentService(paymentRepository, govPayConfig, govPayClient);

    @Test
    public void resolvesAuthorizationKeyWhenRetrieving() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(paymentRepository.findByGovPayId("govPayId")).thenReturn(Optional.of(paymentWith().selfUrl("selfUrl").build()));
        govPayPaymentService.retrieve("divorce", "govPayId");
        verify(govPayClient).retrievePayment("authorizationKey", "selfUrl");
    }

    @Test
    public void resolvesAuthorizationKeyWhenCreating() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        govPayPaymentService.create("divorce", "applicationReference", 100, "email", "reference", "description", "returnUrl");
        verify(govPayClient).createPayment("authorizationKey", new CreatePaymentRequest(100, "reference", "description", "returnUrl"));
    }

    @Test
    public void resolvesAuthorizationKeyWhenCanceling() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(paymentRepository.findByGovPayId("govPayId")).thenReturn(Optional.of(paymentWith().cancelUrl("cancelUrl").build()));
        govPayPaymentService.cancel("divorce", "govPayId");
        verify(govPayClient).cancelPayment("authorizationKey", "cancelUrl");
    }

    @Test
    public void resolvesAuthorizationKeyWhenRefunding() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(paymentRepository.findByGovPayId("govPayId")).thenReturn(Optional.of(paymentWith().refundsUrl("refundsUrl").build()));
        govPayPaymentService.refund("divorce", "govPayId", 100, 500);
        verify(govPayClient).refundPayment("authorizationKey", "refundsUrl", new RefundPaymentRequest(100, 500));
    }
}