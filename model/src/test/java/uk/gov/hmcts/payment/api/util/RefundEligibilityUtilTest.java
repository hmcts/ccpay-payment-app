package uk.gov.hmcts.payment.api.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
    Date paymentUpdateDate = date.getIsoDateTimeFormatter().parseDateTime("2021-11-02T21:48:07")
        .toDate();

    @Autowired
    private RefundEligibilityUtil refundEligibilityUtil;

    @Test
    public void returnTrueWhenPaymentMethodIsCardAndPaymentUpdatedDateIsAfterLagTime(){
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 121);
        Assert.assertEquals(isRefundEligible, true);
    }

    @Test
    public void returnFalseWhenPaymentMethodIsCardAndPaymentUpdatedDateIsBeforeLagTime(){
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 119);
        Assert.assertEquals(isRefundEligible, false);
    }

    @Test
    public void returnTrueWhenPaymentMethodIsCashAndPaymentUpdatedDateIsAfterLagTime(){
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cash").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 121);
        Assert.assertEquals(isRefundEligible, true);
    }

    @Test
    public void returnFalseWhenPaymentMethodIsCashAndPaymentUpdatedDateIsBeforeLagTime(){
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("cash").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 119);
        Assert.assertEquals(isRefundEligible, false);
    }

    @Test
    public void returnTrueWhenPaymentMethodIsPBAAndPaymentUpdatedDateIsAfterLagTime(){
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 97);
        Assert.assertEquals(isRefundEligible, true);
    }

    @Test
    public void returnFalseWhenPaymentMethodIsPBAAndPaymentUpdatedDateIsBeforeLagTime() {
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 95);
        Assert.assertEquals(isRefundEligible, false);
    }


    @Test
    public void returnTrueWhenPaymentMethodIsPostalOrderAndPaymentUpdatedDateIsAfterLagTime() {
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("postal order").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 481);
        Assert.assertEquals(isRefundEligible, true);
    }

    @Test
    public void returnFalseWhenPaymentMethodIsPostalOrderAndPaymentUpdatedDateIsBeforeLagTime(){
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("postal order").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 479);
        Assert.assertEquals(isRefundEligible, false);
    }

    @Test
    public void returnTrueWhenPaymentMethodIsChequeAndPaymentUpdatedDateIsAfterLagTime(){
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("postal order").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 481);
        Assert.assertEquals(isRefundEligible, true);
    }

    @Test
    public void returnFalseWhenPaymentMethodIsChequeAndPaymentUpdatedDateIsBeforeLagTime() {
//Given
        Payment payment = Payment.paymentWith()
            .paymentMethod(PaymentMethod.paymentMethodWith().name("postal order").build())
            .dateUpdated(paymentUpdateDate)
            .build();
        boolean isRefundEligible = refundEligibilityUtil.getRefundEligiblityStatus(payment, 479);
        Assert.assertEquals(isRefundEligible, false);
    }
}
