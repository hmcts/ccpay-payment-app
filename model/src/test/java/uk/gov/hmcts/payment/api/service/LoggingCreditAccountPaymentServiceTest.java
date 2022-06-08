package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class LoggingCreditAccountPaymentServiceTest {

    @Mock
    private UserIdSupplier userIdSupplier;

    @Mock
    private CreditAccountPaymentService creditAccountPaymentService;

    @InjectMocks
    private LoggingCreditAccountPaymentService loggingCreditAccountPaymentService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createCreditAccountPaymentTest() throws Exception {
        when(userIdSupplier.get()).thenReturn("USER_ID");
        String userId = userIdSupplier.get();

        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("10000"))
            .reference("RC-1518-9479-8089-4415")
            .currency("GBP")
            .pbaNumber("pbaNumber")
            .customerReference("customerReference")
            .status("Initiated")
            .organisationName("organisationName")
            .build();
        List<PaymentFee> fees = Arrays.asList(PaymentFee.feeWith()
            .calculatedAmount(new BigDecimal("10000"))
            .code("X0001")
            .version("1")
            .build());

        when(creditAccountPaymentService.create(payment, fees, "2018-1234567890")).thenReturn(PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-1234567890")
            .payments(Arrays.asList(Payment.paymentWith()
                .id(1)
                .userId(userId)
                .amount(new BigDecimal("10000"))
                .reference("RC-1518-9479-8089-4415")
                .currency("GBP")
                .pbaNumber("pbaNumber")
                .customerReference("customerReference")
                .status("Initiated")
                .organisationName("organisationName")
                .build()))
            .fees(Arrays.asList(PaymentFee.feeWith()
                .id(1)
                .calculatedAmount(new BigDecimal("10000"))
                .code("X0001")
                .version("1")
                .build()))
            .build());

        PaymentFeeLink paymentFeeLink = loggingCreditAccountPaymentService.create(payment, fees, "2018-1234567890");
        assertNotNull(paymentFeeLink);
        assertEquals(paymentFeeLink.getPaymentReference(), "2018-1234567890");
        paymentFeeLink.getPayments().stream().forEach(p -> {
            assertEquals(p.getAmount(), new BigDecimal("10000"));
            assertEquals(p.getStatus(), "Initiated");
            assertEquals(p.getReference(), "RC-1518-9479-8089-4415");
        });
    }

    @Test
    public void testDelete() {
        loggingCreditAccountPaymentService.deleteByPaymentReference("");
    }
}
