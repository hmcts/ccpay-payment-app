package uk.gov.hmcts.payment.api.componenttests;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.componenttests.TestUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class PaymentServiceTest extends TestUtil {

    @Autowired
    protected PaymentService paymentService;

    private PaymentsDataUtil paymentsDataUtil;

    @Test
    public void testListInitiatedStatusPaymentsReferences() throws Exception {
        paymentsDataUtil = new PaymentsDataUtil();
        // given DB has 4 payments
        List<Payment> payments = Lists.newArrayList(
            getPaymentWithStatus("created"),
            getPaymentWithStatus("submitted"),
            // final states
            getPaymentWithStatus("success"),
            getPaymentWithStatus("cancelled"),
            getPaymentWithStatus("failed"),
            getPaymentWithStatus("error"));

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .payments(payments)
            .fees(paymentsDataUtil.getFeesData())
            .build();

        paymentFeeLinkRepository.save(paymentFeeLink);

        // when
        List<Reference> paymentsReferences = paymentService.listInitiatedStatusPaymentsReferences();

        // then
         assertThat(paymentsReferences).hasSize(2);
    }

    private Payment getPaymentWithStatus(String paymentStatus) {
        return Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("CMC")
            .currency("GBP")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name(paymentStatus).build())
            .reference("RC-1519-9028-2432-0002" + paymentStatus)
            .build();
    }
}
