package uk.gov.hmcts.payment.api.service;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

public class LoggingPaymentRecordServiceTest {

    @Mock
    private PaymentRecordService<PaymentFeeLink, String> delegate;

    @Mock
    private UserIdSupplier userIdSupplier;

    @InjectMocks
    private LoggingPaymentRecordService loggingPaymentRecordService;



    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRecordPayment() throws Exception {
        String paymentGroupRef = "2018-12345678";
        List<PaymentFee> fees = Arrays.asList(getFee());
        Payment payment = getPayment();

        when(userIdSupplier.get()).thenReturn("USER_ID");
        String userId = userIdSupplier.get();
        payment.setUserId(userId);

        when(delegate.recordPayment(payment, fees, paymentGroupRef)).thenReturn(getPaymentFeeLink());
        PaymentFeeLink paymentFeeLink = loggingPaymentRecordService.recordPayment(payment, fees, paymentGroupRef);

        assertThat(paymentFeeLink.getPaymentReference()).isEqualTo(paymentGroupRef);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);
        assertThat(savedPayment.getAmount()).isEqualTo(new BigDecimal("100.11"));
        assertThat(savedPayment.getReference()).isEqualTo("RC-1528-8964-9212-8451");
        assertThat(savedPayment.getGiroSlipNo()).isEqualTo("G12345");

        PaymentFee savedFee = paymentFeeLink.getFees().get(0);
        assertThat(savedFee.getCalculatedAmount()).isEqualTo(new BigDecimal("100.11"));
        assertThat(savedFee.getCode()).isEqualTo("FEE0001");
        assertThat(savedFee.getVolume()).isEqualTo(1);
        assertThat(savedFee.getVersion()).isEqualTo("1");
    }


    private PaymentFeeLink getPaymentFeeLink() {
        return PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-12345678")
            .payments(Arrays.asList(getPayment()))
            .fees(Arrays.asList(getFee()))
            .build();
    }

    private Payment getPayment() {
        return Payment.paymentWith()
            .id(1)
            .reference("RC-1528-8964-9212-8451")
            .amount(new BigDecimal("100.11"))
            .ccdCaseNumber("ccdCaseNumber")
            .currency("GBP")
            .giroSlipNo("G12345")
            .siteId("siteID")
            .userId("userId")
            .build();
    }

    private PaymentFee getFee() {
        return PaymentFee.feeWith()
            .id(1)
            .code("FEE0001")
            .version("1")
            .calculatedAmount(new BigDecimal("100.11"))
            .volume(1)
            .build();
    }


}
