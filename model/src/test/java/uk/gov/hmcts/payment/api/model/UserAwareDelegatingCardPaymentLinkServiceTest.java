package uk.gov.hmcts.payment.api.model;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment.govPaymentWith;
import static uk.gov.hmcts.payment.api.model.Payment.*;


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
    private PaymentFeeLinkRepository paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);

    private UserAwareDelegatingCardPaymentService cardPaymentService = new UserAwareDelegatingCardPaymentService(() -> USER_ID, paymentFeeLinkRepository,
        govPayCardPaymentService, paymentChannelRepository, paymentMethodRepository, paymentProviderRepository, paymentStatusRepository);

    @Test
    public void checkCreateWiring() {
        when(govPayCardPaymentService.create(10, "paymentReference", "description", "returnUrl",
            "ccdCaseNo", "caseReference", "GBP", "siteId",
            Arrays.asList(Fee.feeWith().code("code").version("version").amount(new BigDecimal(1000)).build()))).thenReturn(VALID_GOV_PAYMENT_RESPONSE);

        when(paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("paymentReference")
            .payments(Arrays.asList(Payment.paymentWith().govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).userId(USER_ID)
                .amount(BigDecimal.valueOf(1000).movePointRight(2)).description("description").returnUrl("returnUrl").build()))
            .fees(Arrays.asList(Fee.feeWith().code("code").version("version").amount(new BigDecimal(1000)).build()))
            .build()))
            .thenReturn(PaymentFeeLink.paymentFeeLinkWith().id(999).paymentReference("paymentReference")
                .payments(Arrays.asList(Payment.paymentWith().id(998).govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).userId(USER_ID)
                    .amount(BigDecimal.valueOf(1000).movePointRight(2)).description("description").returnUrl("returnUrl").build()))
                .fees(Arrays.asList(Fee.feeWith().id(998).code("feeCode").version("feeVersion").amount(new BigDecimal(1000)).build()))
                .build());
    }


    private Payment mapToPayment(Integer id, String govPayId, GovPayPayment govPayPayment) {
        return paymentWith()
            .id(id)
            .govPayId(govPayId)
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
