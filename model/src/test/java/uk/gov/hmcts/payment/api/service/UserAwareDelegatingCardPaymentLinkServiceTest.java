package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
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
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment.govPaymentWith;

public class UserAwareDelegatingCardPaymentLinkServiceTest {

    private final static String PAYMENT_METHOD = "card";

    private static final String USER_ID = "USER_ID";

    private ReferenceUtil referenceUtil = mock(ReferenceUtil.class);
    private PaymentChannelRepository paymentChannelRepository = mock(PaymentChannelRepository.class);
    private PaymentMethodRepository paymentMethodRepository = mock(PaymentMethodRepository.class);
    private PaymentProviderRepository paymentProviderRepository = mock(PaymentProviderRepository.class);
    private PaymentStatusRepository paymentStatusRepository = mock(PaymentStatusRepository.class);

    private DelegatingPaymentService<GovPayPayment, String> govPayDelegatingPaymentService = mock(DelegatingPaymentService.class);
    private DelegatingPaymentService<PciPalPayment, String> pciPalDelegatingPaymentService = mock(DelegatingPaymentService.class);
    private Payment2Repository paymentRespository = mock(Payment2Repository.class);
    private PaymentFeeLinkRepository paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);

    private GovPayAuthUtil govPayAuthUtil = mock(GovPayAuthUtil.class);
    private ServiceIdSupplier serviceIdSupplier = mock(ServiceIdSupplier.class);
    private AuditRepository auditRepository = mock(AuditRepository.class);

    private CallbackService callbackService = mock(CallbackService.class);

    private UserAwareDelegatingPaymentService cardPaymentService = new UserAwareDelegatingPaymentService(() -> USER_ID, paymentFeeLinkRepository,
        govPayDelegatingPaymentService, pciPalDelegatingPaymentService, paymentChannelRepository, paymentMethodRepository, paymentProviderRepository,
        paymentStatusRepository, paymentRespository, referenceUtil, govPayAuthUtil, serviceIdSupplier, auditRepository, callbackService);

    @Test
    public void testRetrieveWhenServiceCallbackUrlIsDefinedCallbackServiceIsInvoked() throws Exception {

        String serviceName = "cmc";
        when(referenceUtil.getNext("RC")).thenReturn("RC-1234-1234-1234-123C");
        when(govPayAuthUtil.getServiceName(null, serviceName)).thenReturn(serviceName);
        String reference = referenceUtil.getNext("RC");

        Payment payment1 = Payment.paymentWith().id(1)
            .externalReference("govPayId")
            .serviceType(serviceName)
            .s2sServiceName(serviceName)
            .serviceCallbackUrl("http://pandito.com")
            .reference(reference)
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .id(1)
                .status("Initiated")
                .externalStatus("created")
                .build()))
            .build();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink
            .paymentFeeLinkWith()
            .id(1)
            .paymentReference("payGroupRef")
            .payments(Arrays.asList(payment1))
            .build();

        payment1.setPaymentLink(paymentFeeLink);

        when(paymentFeeLinkRepository.findByPaymentReference("1")).thenReturn(Optional.of(paymentFeeLink));

        paymentFeeLinkRepository.findByPaymentReference("1").orElseThrow(PaymentNotFoundException::new);

        when(paymentRespository.findByReferenceAndPaymentMethod(reference,
            PaymentMethod.paymentMethodWith().name(PAYMENT_METHOD).build()))
            .thenReturn(Optional.of(payment1));

        paymentRespository.findByReferenceAndPaymentMethod(reference,
            PaymentMethod.paymentMethodWith().name(PAYMENT_METHOD).build()).orElseThrow(PaymentNotFoundException::new);

        when(govPayDelegatingPaymentService.retrieve("govPayId", "cmc")).thenReturn(ANOTHER_GOV_PAYMENT_RESPONSE);

        PaymentFeeLink result = cardPaymentService.retrieve(reference);

        verify(callbackService, times(1)).callback(any(), any());

        assertNotNull(result.getPayments().get(0));

    }

    @Test
    public void testRetrieveCardPaymentForGivenPaymentReference() throws Exception {
        String serviceName = "cmc";
        when(referenceUtil.getNext("RC")).thenReturn("RC-1234-1234-1234-123C");
        when(govPayAuthUtil.getServiceName(null, serviceName)).thenReturn(serviceName);
        String reference = referenceUtil.getNext("RC");

        when(paymentFeeLinkRepository.findByPaymentReference("1")).thenReturn(Optional.of(PaymentFeeLink.paymentFeeLinkWith().id(1).paymentReference("payGroupRef")
            .payments(Arrays.asList(Payment.paymentWith().id(1)
                .externalReference("govPayId")
                .serviceType(serviceName)
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

        when(govPayDelegatingPaymentService.retrieve("govPayId", "cmc")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);

        verify(callbackService, times(0)).callback(any(), any());

        PaymentFeeLink result = cardPaymentService.retrieve(reference);
        assertNotNull(result.getPayments().get(0));
        assertEquals(result.getPayments().get(0).getExternalReference(), "govPayId");
        assertEquals(result.getPayments().get(0).getReference(), reference);
    }

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

    private static final GovPayPayment ANOTHER_GOV_PAYMENT_RESPONSE = govPaymentWith()
        .paymentId("paymentId")
        .amount(1000)
        .description("description")
        .reference("paymentReference")
        .state(new State("failed", true, "message", "code"))
        .links(new GovPayPayment.Links(
            new Link("type", ImmutableMap.of(), "self", "GET"),
            new Link("type", ImmutableMap.of(), "nextUrl", "GET"),
            new Link("type", ImmutableMap.of(), "nextUrlPost", "GET"),
            new Link("type", ImmutableMap.of(), "eventsUrl", "GET"),
            new Link("type", ImmutableMap.of(), "refundsUrl", "GET"),
            new Link("type", ImmutableMap.of(), "cancelUrl", "GET")
        ))
        .build();


}
