package uk.gov.justice.payment.api.services;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Test;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.GovPayPayment;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.State;
import uk.gov.justice.payment.api.model.Payment;
import uk.gov.justice.payment.api.model.PaymentRepository;
import uk.gov.justice.payment.api.model.govpay.GovPayConfig;
import uk.gov.justice.payment.api.model.govpay.GovPayPaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.justice.payment.api.external.client.dto.GovPayPayment.govPaymentWith;
import static uk.gov.justice.payment.api.model.Payment.paymentWith;


public class GovPayPaymentServiceTest {

    private static final GovPayPayment VALID_GOV_PAYMENT_RESPONSE = govPaymentWith()
            .paymentId("paymentId")
            .amount(100)
            .description("description")
            .reference("reference")
            .state(new State("status", false, "message", "code"))
            .links(new GovPayPayment.Links(
                    new Link("type", ImmutableMap.of(), "selfUrl", "GET"),
                    new Link("type", ImmutableMap.of(), "nextUrl", "GET"),
                    new Link("type", ImmutableMap.of(), "nextUrlPost", "GET"),
                    new Link("type", ImmutableMap.of(), "eventsUrl", "GET"),
                    new Link("type", ImmutableMap.of(), "refundsUrl", "GET"),
                    new Link("type", ImmutableMap.of(), "cancelUrl", "GET")
            ))
            .build();

    private PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private GovPayConfig govPayConfig = mock(GovPayConfig.class);
    private GovPayClient govPayClient = mock(GovPayClient.class);
    private GovPayPaymentService govPayPaymentService = new GovPayPaymentService(paymentRepository, govPayConfig, govPayClient);

    @Test
    public void checkRetrieveWiring() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(paymentRepository.findById(1)).thenReturn(Optional.of(paymentWith().id(1).govPayId("govPayId").build()));
        when(govPayClient.retrievePayment("authorizationKey", "govPayId")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        assertThat(govPayPaymentService.retrieve("divorce", 1)).isEqualTo(mapToPayment(1, "govPayId", VALID_GOV_PAYMENT_RESPONSE));
    }

    @Test
    public void checkCreateWiring() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(govPayClient.createPayment("authorizationKey", new CreatePaymentRequest(100, "reference", "description", "returnUrl"))).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        when(paymentRepository.save(paymentWith().govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).serviceId("divorce").build()))
                .thenReturn(paymentWith().id(999).govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).build());

        Payment payment = govPayPaymentService.create("divorce", 100, "reference", "description", "returnUrl");
        assertThat(payment).isEqualTo(mapToPayment(999, VALID_GOV_PAYMENT_RESPONSE.getPaymentId(), VALID_GOV_PAYMENT_RESPONSE));
    }

    @Test
    public void checkCancelWiring() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(paymentRepository.findById(1)).thenReturn(Optional.of(paymentWith().govPayId("govPayId").build()));
        when(govPayClient.retrievePayment("authorizationKey", "govPayId")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        govPayPaymentService.cancel("divorce", 1);
        verify(govPayClient).cancelPayment("authorizationKey", VALID_GOV_PAYMENT_RESPONSE.getLinks().getCancel().getHref());
    }

    @Test
    public void resolvesAuthorizationKeyWhenRefunding() {
        when(govPayConfig.getKeyForService("divorce")).thenReturn("authorizationKey");
        when(paymentRepository.findById(1)).thenReturn(Optional.of(paymentWith().govPayId("govPayId").build()));
        when(govPayClient.retrievePayment("authorizationKey", "govPayId")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        govPayPaymentService.refund("divorce", 1, 100, 500);
        verify(govPayClient).refundPayment("authorizationKey", VALID_GOV_PAYMENT_RESPONSE.getLinks().getRefunds().getHref(), new RefundPaymentRequest(100, 500));
    }

    private Payment mapToPayment(Integer id, String govPayId, GovPayPayment govPayPayment) {
        return paymentWith()
                .id(id)
                .govPayId(govPayId)
                .amount(govPayPayment.getAmount())
                .description(govPayPayment.getDescription())
                .reference(govPayPayment.getReference())
                .status(govPayPayment.getState().getStatus())
                .finished(govPayPayment.getState().getFinished())
                .nextUrl(govPayPayment.getLinks().getNextUrl().getHref())
                .cancelUrl(govPayPayment.getLinks().getCancel().getHref())
                .refundsUrl(govPayPayment.getLinks().getRefunds().getHref())
                .build();
    }
}