package uk.gov.hmcts.payment.api.model;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment.govPaymentWith;
import static uk.gov.hmcts.payment.api.model.Payment.*;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class UserAwareDelegatingCardPaymentLinkServiceTest {

    private static final String USER_ID = "USER_ID";
    private static final GovPayPayment VALID_GOV_PAYMENT_RESPONSE = govPaymentWith()
        .paymentId("paymentId")
        .amount(1000)
        .description("description")
        .reference("paymentReference")
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

    private PaymentChannelRepository paymentChannelRepository = mock(PaymentChannelRepository.class);
    private PaymentMethodRepository paymentMethodRepository = mock(PaymentMethodRepository.class);
    private PaymentProviderRepository paymentProviderRepository = mock(PaymentProviderRepository.class);
    private PaymentStatusRepository paymentStatusRepository = mock(PaymentStatusRepository.class);

    private CardPaymentService<GovPayPayment, String> govPayCardPaymentService = mock(CardPaymentService.class);
    private Payment2Repository paymentRespository = mock(Payment2Repository.class);
    private PaymentFeeLinkRepository paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);

    private UserAwareDelegatingCardPaymentService cardPaymentService = new UserAwareDelegatingCardPaymentService(() -> USER_ID, paymentFeeLinkRepository,
        govPayCardPaymentService, paymentChannelRepository, paymentMethodRepository, paymentProviderRepository, paymentStatusRepository, paymentRespository);

    @Test
    public void checkCreateWiring() throws CheckDigitException {
        when(govPayCardPaymentService.create(10, "paymentReference", "description", "returnUrl",
            "ccdCaseNo", "caseReference", "GBP", "siteId", "CMC1",
            Arrays.asList(Fee.feeWith().code("code").version("version").amount(new BigDecimal(1000)).build()))).thenReturn(VALID_GOV_PAYMENT_RESPONSE);

        when(paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("paymentReference")
            .payments(Arrays.asList(Payment.paymentWith().externalReference(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).userId(USER_ID)
                .amount(BigDecimal.valueOf(1000).movePointRight(2)).description("description").returnUrl("returnUrl").build()))
            .fees(Arrays.asList(Fee.feeWith().code("code").version("version").amount(new BigDecimal(1000)).build()))
            .build()))
            .thenReturn(PaymentFeeLink.paymentFeeLinkWith().id(999).paymentReference("paymentReference")
                .payments(Arrays.asList(Payment.paymentWith().id(998).externalReference(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).userId(USER_ID)
                    .amount(BigDecimal.valueOf(1000).movePointRight(2)).description("description").returnUrl("returnUrl").build()))
                .fees(Arrays.asList(Fee.feeWith().id(998).code("feeCode").version("feeVersion").amount(new BigDecimal(1000)).build()))
                .build());
    }

    @Test
    public void testRetrieveCardPaymentForGivenPaymentReference() throws Exception {
        when(paymentFeeLinkRepository.findByPaymentReference("1")).thenReturn(Optional.of(PaymentFeeLink.paymentFeeLinkWith().id(1).paymentReference("payGroupRef")
            .payments(Arrays.asList(Payment.paymentWith().id(1).externalReference("govPayId").reference("RC-1234-1234-1234-123C").build())).build()));

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference("1").orElseThrow(PaymentNotFoundException::new);
        when(paymentRespository.findByReference("RC-1234-1234-1234-123C")).thenReturn(Optional.of(Payment.paymentWith().id(1)
            .externalReference("govPayId")
            .reference("RC-1234-1234-1234-123C")
            .paymentLink(paymentFeeLink)
            .build()));

        Payment payment = paymentRespository.findByReference("RC-1234-1234-1234-123C").orElseThrow(PaymentNotFoundException::new);

        when(govPayCardPaymentService.retrieve("govPayId")).thenReturn(VALID_GOV_PAYMENT_RESPONSE);

        PaymentFeeLink result = cardPaymentService.retrieve("RC-1234-1234-1234-123C");
        assertNotNull(result.getPayments().get(0));
        assertEquals(result.getPayments().get(0).getExternalReference(), "govPayId");
        assertEquals(result.getPayments().get(0).getReference(), "RC-1234-1234-1234-123C");
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
