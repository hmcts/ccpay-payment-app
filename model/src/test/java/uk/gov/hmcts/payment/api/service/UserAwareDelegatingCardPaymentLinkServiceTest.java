package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentProviderRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment.govPaymentWith;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;

@RunWith(MockitoJUnitRunner.class)
public class UserAwareDelegatingCardPaymentLinkServiceTest {

    private final static String PAYMENT_METHOD = "card";

    private static final String USER_ID = "USER_ID";
    private static final GovPayPayment VALID_GOV_PAYMENT_RESPONSE = govPaymentWith()
        .paymentId("paymentId")
        .amount(1000)
        .description("description")
        .reference("paymentReference")
        .state(new State("created", false, "message", "code"))
        .links(new GovPayPayment.Links(
            new Link("type", ImmutableMap.of(), "self", "GET"),
            new Link("type", ImmutableMap.of(), "nextUrl", "GET"),
            new Link("type", ImmutableMap.of(), "nextUrlPost", "GET"),
            new Link("type", ImmutableMap.of(), "eventsUrl", "GET"),
            new Link("type", ImmutableMap.of(), "refundsUrl", "GET"),
            new Link("type", ImmutableMap.of(), "cancelUrl", "GET")
        ))
        .build();

    private PaymentReferenceUtil paymentReferenceUtil = mock(PaymentReferenceUtil.class);
    private PaymentChannelRepository paymentChannelRepository = mock(PaymentChannelRepository.class);
    private PaymentMethodRepository paymentMethodRepository = mock(PaymentMethodRepository.class);
    private PaymentProviderRepository paymentProviderRepository = mock(PaymentProviderRepository.class);
    private PaymentStatusRepository paymentStatusRepository = mock(PaymentStatusRepository.class);

    private CardPaymentService<GovPayPayment, String> govPayCardPaymentService = mock(CardPaymentService.class);
    private Payment2Repository paymentRespository = mock(Payment2Repository.class);
    private PaymentFeeLinkRepository paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);

    private UserAwareDelegatingCardPaymentService cardPaymentService = new UserAwareDelegatingCardPaymentService(() -> USER_ID, paymentFeeLinkRepository,
        govPayCardPaymentService, paymentChannelRepository, paymentMethodRepository, paymentProviderRepository,
        paymentStatusRepository, paymentRespository, paymentReferenceUtil);

    @Test
    public void testRetrieveCardPaymentForGivenPaymentReference() throws Exception {
        when(paymentReferenceUtil.getNext()).thenReturn("RC-1234-1234-1234-123C");
        String reference = paymentReferenceUtil.getNext();

        when(paymentFeeLinkRepository.findByPaymentReference("1")).thenReturn(Optional.of(PaymentFeeLink.paymentFeeLinkWith().id(1).paymentReference("payGroupRef")
            .payments(Arrays.asList(Payment.paymentWith().id(1)
                .externalReference("govPayId")
                .serviceType("cmc")
                .reference(reference)
                .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                    .id(1)
                    .status("Initiated")
                    .externalStatus("created")
                    .build()))
                .build()))
            .build()));

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference("1").orElseThrow(PaymentNotFoundException::new);
        when(paymentRespository.findByReferenceAndPaymentMethod(reference, PaymentMethod.paymentMethodWith().name(PAYMENT_METHOD).build())).thenReturn(Optional.of(Payment.paymentWith().id(1)
            .externalReference("govPayId")
            .serviceType("cmc")
            .reference(reference)
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .id(1)
                .status("Initiated")
                .externalStatus("created")
                .build()))
            .paymentLink(paymentFeeLink)
            .build()));

        Payment payment = paymentRespository.findByReferenceAndPaymentMethod(reference,
            PaymentMethod.paymentMethodWith().name(PAYMENT_METHOD).build()).orElseThrow(PaymentNotFoundException::new);

        when(govPayCardPaymentService.retrieve("govPayId", "cmc")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);

        PaymentFeeLink result = cardPaymentService.retrieve(reference);
        assertNotNull(result.getPayments().get(0));
        assertEquals(result.getPayments().get(0).getExternalReference(), "govPayId");
        assertEquals(result.getPayments().get(0).getReference(), reference);
    }


    private Payment mapToPayment(Integer id, String govPayId, GovPayPayment govPayPayment) {
        return paymentWith()
            .id(id)
            .externalReference(govPayId)
            .amount(BigDecimal.valueOf(govPayPayment.getAmount()).movePointRight(2))
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
