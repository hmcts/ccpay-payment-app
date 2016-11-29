package uk.gov.justice.payment.api.services;

import org.junit.Test;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.RefundPaymentRequest;

import static org.mockito.Mockito.*;


public class GovPayPaymentServiceTest {

    private GovPayConfig govPayConfig = mock(GovPayConfig.class);
    private GovPayClient govPayClient = mock(GovPayClient.class);
    private GovPayPaymentService govPayPaymentService = new GovPayPaymentService(govPayConfig, govPayClient);

    @Test
    public void resolvesAuthorizationKeyWhenRetrieving() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        govPayPaymentService.retrieve("divorce", "paymentId");
        verify(govPayClient).retrievePayment("authorizationKey", "paymentId");
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
        govPayPaymentService.cancel("divorce", "paymentId");
        verify(govPayClient).cancelPayment("authorizationKey", "paymentId");
    }

    @Test
    public void resolvesAuthorizationKeyWhenRefunding() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        govPayPaymentService.refund("divorce", "paymentId", 100, 500);
        verify(govPayClient).refundPayment("authorizationKey", "paymentId", new RefundPaymentRequest(100, 500));
    }
}