package uk.gov.hmcts.payment.api.model;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment.govPaymentWith;
import static uk.gov.hmcts.payment.api.model.Payment.*;


public class UserAwareDelegatingCardPaymentLinkServiceTest {

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

    private PaymentChannelRepository paymentChannelRepository = mock(PaymentChannelRepository.class);
    private PaymentMethodRepository paymentMethodRepository = mock(PaymentMethodRepository.class);
    private PaymentProviderRepository paymentProviderRepository = mock(PaymentProviderRepository.class);
    private PaymentStatusRepository paymentStatusRepository = mock(PaymentStatusRepository.class);

    private Payment2Service<GovPayPayment, String> govPayPayment2Service = mock(Payment2Service.class);
    private PaymentFeeLinkRepository paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);

    private UserAwareDelegatingCardPaymentService cardPaymentService = new UserAwareDelegatingCardPaymentService(() -> USER_ID, paymentFeeLinkRepository,
        govPayPayment2Service, paymentChannelRepository, paymentMethodRepository, paymentProviderRepository, paymentStatusRepository);

    @Test
    public void checkCreateWiring() {
        Fee fee = Fee.feeWith().code("feeCode").version("feeVersion").build();

        when(paymentChannelRepository.findByNameOrThrow("online")).thenReturn(PaymentChannel.paymentChannelWith().name("online").build());
        when(paymentMethodRepository.findByNameOrThrow("card")).thenReturn(PaymentMethod.paymentMethodWith().name("card").build());
        when(paymentProviderRepository.findByNameOrThrow("gov pay")).thenReturn(PaymentProvider.paymentProviderWith().name("gov pay").build());
        when(paymentStatusRepository.findByNameOrThrow("created")).thenReturn(PaymentStatus.paymentStatusWith().name("created").build());

        when(govPayPayment2Service.create(100, "reference", "description", "returnUrl", "ccdCaseNo",
            "caseRef", "GBP", "siteId", Arrays.asList(fee))).thenReturn(VALID_GOV_PAYMENT_RESPONSE);

       when(paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("paymentReference")
           .payments(Arrays.asList(Payment.paymentWith().govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).userId(USER_ID).build()))
           .fees(Arrays.asList(getFee()))
           .build())).thenReturn(PaymentFeeLink.paymentFeeLinkWith().id(999).paymentReference("paymentReference")
               .payments(Arrays.asList(Payment.paymentWith().id(888).govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId()).build()))
               .fees(Arrays.asList(Fee.feeWith().id(777).code("feeCode").version("feeVersion").build())).build());

       PaymentFeeLink paymentFeeLink = cardPaymentService.create(100, "reference", "description", "returnUrl",
           "ccdCaseNo","caseRef", "GBP", "siteId", Arrays.asList(fee));

        //System.out.println("PaymentLink : " + paymentFeeLink.getId());

    }

    private Fee getFee() {
        return Fee.feeWith().code("feeCode").version("feeVersion").build();
    }

    private Fee getFeeWithId() {
        return Fee.feeWith().id(998).code("feeCode").version("feeVersion").build();
    }

    private Payment getPayment() {
        return Payment.paymentWith().govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId())
            .amount(100).reference("reference").description("description").returnUrl("returnUrl")
            .ccdCaseNumber("ccdCaseNo").caseReference("caseRef").currency("GBP").siteId("siteId")
            .build();
    }

    private Payment getPaymentWithId() {
        return Payment.paymentWith().id(998).govPayId(VALID_GOV_PAYMENT_RESPONSE.getPaymentId())
            .amount(100).reference("reference").description("description").returnUrl("returnUrl")
            .ccdCaseNumber("ccdCaseNo").caseReference("caseRef").currency("GBP").siteId("siteId")
            .build();
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
