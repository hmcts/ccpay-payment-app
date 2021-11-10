package uk.gov.hmcts.payment.api.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentMethod;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RefundEligibilityUtil.class)
@TestPropertySource("classpath:application-test.properties")

public class RefundEligibilityUtilTest {

    private final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MM-yyyy");
    DateUtil date = new DateUtil();
    Date paymentCreationDate = date.getIsoDateTimeFormatter().parseDateTime("2021-11-02T21:48:07")
        .toDate();

    @Value("${card.lagdays}")
    private Integer cardLagDays;
    @Value("${cash.lagdays}")
    private Integer cashLagDays;
    @Value("${postal_orders.lagdays}")
    private Integer postalOrderLagDays;
    @Value("${cheques.lagdays}")
    private Integer chequesLagDays;
    @Value("${pba.lagdays}")
    private Integer pbaLagDays;

    @Autowired
    private RefundEligibilityUtil refundEligibilityUtil;

    @Test
    public void testPaymentRefundEligibleDateforCardMethod() throws ReflectiveOperationException {
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .dateCreated(paymentCreationDate)
            .build();

        Date adjuestedDate = refundEligibilityUtil.getRefundEligiblityStatus(payment);
        String strDate = DATE_FORMATTER.format(adjuestedDate);
        Assert.assertEquals(strDate, "07-11-2021");
    }

    @Test
    public void testPaymentRefundEligibleDateforCashMethod() {

//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cash").build())
            .dateCreated(paymentCreationDate)
            .build();

        Date adjuestedDate = refundEligibilityUtil.getRefundEligiblityStatus(payment);
        String strDate = DATE_FORMATTER.format(adjuestedDate);
        Assert.assertEquals(strDate, "07-11-2021");
    }

    @Test
    public void testPaymentRefundEligibleDateforPBAMethod() {

//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .dateCreated(paymentCreationDate)
            .build();

        Date adjuestedDate = refundEligibilityUtil.getRefundEligiblityStatus(payment);
        String strDate = DATE_FORMATTER.format(adjuestedDate);
        Assert.assertEquals(strDate, "06-11-2021");
    }

    @Test
    public void testPaymentRefundEligibleDateforPostalOrdersMethod() {

//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("postal order").build())
            .dateCreated(paymentCreationDate)
            .build();

        Date adjuestedDate = refundEligibilityUtil.getRefundEligiblityStatus(payment);
        String strDate = DATE_FORMATTER.format(adjuestedDate);
        Assert.assertEquals(strDate, "22-11-2021");
    }

    @Test
    public void testPaymentRefundEligibleDateforChequesMethod() {

//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cheque").build())
            .dateCreated(paymentCreationDate)
            .build();

        Date adjuestedDate = refundEligibilityUtil.getRefundEligiblityStatus(payment);
        String strDate = DATE_FORMATTER.format(adjuestedDate);
        Assert.assertEquals(strDate, "22-11-2021");
    }

}
