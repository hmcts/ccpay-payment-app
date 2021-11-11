package uk.gov.hmcts.payment.api.componenttests;

import org.joda.time.MutableDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.TestUtil;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
@DirtiesContext(classMode= DirtiesContext.ClassMode.BEFORE_CLASS)
public class CreditAccountPaymentServiceTest extends TestUtil {
    private PaymentsDataUtil paymentsDataUtil;
    @Before
    public void setUp() {
        paymentsDataUtil = new PaymentsDataUtil();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .payments(paymentsDataUtil.getCreditAccountPaymentsData())
            .fees(paymentsDataUtil.getFeesData())
            .build();

        paymentFeeLinkRepository.save(paymentFeeLink);

        //card payments
        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .payments(paymentsDataUtil.getCardPaymentsData())
            .fees(paymentsDataUtil.getFeesData()).build());
    }

//    @After
//    public void tearDown() {
//        paymentsDataUtil = null;
//    }
    @Test
    public void retireveCreditAccountPayments_forBetweenDates_WhereProviderIsMiddleOfficeTest() throws Exception {
        Date fromDate = new Date();
        MutableDateTime mFromDate = new MutableDateTime(fromDate);
        mFromDate.addDays(-1);
        Date toDate = new Date();
        MutableDateTime mToDate = new MutableDateTime(toDate);
        mToDate.addDays(2);

        List<PaymentFeeLink> result = creditAccountPaymentService.search(mFromDate.toDate(), mToDate.toDate());

        assertNotNull(result);
        result.stream().forEach(g -> {
            assertEquals(g.getPayments().size(), 3);
            g.getPayments().stream().forEach(p -> {
                assertEquals(p.getPaymentMethod().getName(), "payment by account");
            });
        });

    }


}
