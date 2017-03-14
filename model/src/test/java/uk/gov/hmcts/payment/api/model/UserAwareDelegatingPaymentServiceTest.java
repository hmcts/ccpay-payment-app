package uk.gov.hmcts.payment.api.model;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Test;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.model.exceptions.PaymentNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment.govPaymentWith;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;

public class UserAwareDelegatingPaymentServiceTest {

    private static final String USER_ID = "USER_ID";
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
    private PaymentService<GovPayPayment, String> govPayPaymentService = mock(PaymentService.class);
    private UserAwareDelegatingPaymentService paymentService = new UserAwareDelegatingPaymentService(() -> USER_ID, paymentRepository, govPayPaymentService);

    @Test
    public void checkCreateWiring() {
        when(govPayPaymentService.create(100, "reference", "description", "returnUrl")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        when(paymentRepository.save(paymentWith().govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).userId(USER_ID).build()))
                .thenReturn(paymentWith().id(999).govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).build());

        Payment payment = paymentService.create(100, "reference", "description", "returnUrl");
        assertThat(payment).isEqualTo(mapToPayment(999, VALID_GOV_PAYMENT_RESPONSE.getPaymentId(), VALID_GOV_PAYMENT_RESPONSE));
    }

    @Test
    public void checkRetrieveWiring() {
        when(paymentRepository.findByUserIdAndId(USER_ID, 1)).thenReturn(Optional.of(paymentWith().id(1).govPayId("govPayId").build()));
        when(govPayPaymentService.retrieve("govPayId")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        assertThat(paymentService.retrieve(1)).isEqualTo(mapToPayment(1, "govPayId", VALID_GOV_PAYMENT_RESPONSE));
    }

    @Test
    public void checkCancelWiring() {
        when(paymentRepository.findByUserIdAndId(USER_ID, 1)).thenReturn(Optional.of(paymentWith().govPayId("govPayId").build()));
        when(govPayPaymentService.retrieve("govPayId")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        paymentService.cancel(1);
        verify(govPayPaymentService).cancel("govPayId");
    }

    @Test
    public void resolvesAuthorizationKeyWhenRefunding() {
        when(paymentRepository.findByUserIdAndId(USER_ID, 1)).thenReturn(Optional.of(paymentWith().govPayId("govPayId").build()));
        when(govPayPaymentService.retrieve("govPayId")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);
        govPayPaymentService.refund("govPayId", 100, 500);
        verify(govPayPaymentService).refund("govPayId", 100, 500);
    }

    @Test(expected = PaymentNotFoundException.class)
    public void unknownUserAndPaymentComboShouldResultInException() {
        when(paymentRepository.findByUserIdAndId(USER_ID, 1)).thenReturn(Optional.empty());
        paymentService.retrieve(1);
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
