package uk.gov.hmcts.payment.api.componenttests;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.ComponentTestBaseUtil;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

public class PaymentRecordComponentTest {


    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";


    @Test
    public void recordCashPaymentTest() throws Exception {
        String paymentGroupRef = "2018-1234567890";
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupRef)
            .payments(Arrays.asList(getPayment())).fees(Arrays.asList(getFee())).build();

        PaymentFeeLink savedPaymentGroup = paymentFeeLinkRepository.save(paymentFeeLink);
        assertThat(savedPaymentGroup.getPaymentReference()).isEqualTo(paymentGroupRef);
        savedPaymentGroup.getPayments().stream().forEach(p -> {
            assertThat(p.getReference().matches(PAYMENT_REFERENCE_REFEX)).isEqualTo(true);
            assertThat(p.getAmount()).isEqualTo(new BigDecimal("6000.00"));
            assertThat(p.getCcdCaseNumber()).isEqualTo("ccdCaseNo_123");
        });
        savedPaymentGroup.getFees().stream().forEach(f -> {
            assertThat(f.getCode()).isEqualTo("FEE0123");
            assertThat(f.getCalculatedAmount()).isEqualTo(new BigDecimal("6000.00"));
            assertThat(f.getVolume()).isEqualTo(1);
            assertThat(f.getVersion()).isEqualTo("1");
        });

    }

    private Payment getPayment() {
        return Payment.paymentWith()
            .amount(new BigDecimal("6000.00"))
            .reference("RC-1234-1234-1234-1112")
            .ccdCaseNumber("ccdCaseNo_123")
            .currency("GBP")
            .siteId("AA_001")
            .serviceType("DIGITAL_BAR")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("digital bar").build())
            .giroSlipNo("giro123")
            .build();
    }


    private PaymentFee getFee() {
        return PaymentFee.feeWith()
            .calculatedAmount(new BigDecimal("6000.00"))
            .code("FEE0123")
            .version("1")
            .volume(1)
            .build();

    }
}
