package uk.gov.hmcts.payment.api.componenttests;

import org.joda.time.MutableDateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.componenttests.TestUtil;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CardPaymentServiceTest extends TestUtil {
    private PaymentsDataUtil paymentsDataUtil;


    @Before
    public void setUp() {
        paymentsDataUtil = new PaymentsDataUtil();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .payments(paymentsDataUtil.getCardPaymentsData())
            .fees(paymentsDataUtil.getFeesData())
            .build();

        paymentFeeLinkRepository.save(paymentFeeLink);

        //pba payments
        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .payments(paymentsDataUtil.getCreditAccountPaymentsData())
            .fees(paymentsDataUtil.getFeesData())
            .build());
    }

    @Test
    public void retireveCardPayments_forBetweenDates_WhereProviderIsGovPayTest() throws Exception {
        Date fromDate = new Date();
        MutableDateTime mFromDate = new MutableDateTime(fromDate);
        mFromDate.addDays(-1);
        Date toDate = new Date();
        MutableDateTime mToDate = new MutableDateTime(toDate);
        mToDate.addDays(2);

        List<PaymentFeeLink> result = cardPaymentService.search(mFromDate.toDate(), mToDate.toDate(), PaymentMethodType.CARD.getType(), null, null);

        assertNotNull(result);
        result.stream().forEach(g -> {
            assertEquals(g.getPayments().size(), 3);
            g.getPayments().stream().forEach(p -> {
                assertEquals(p.getPaymentMethod().getName(), "card");
            });
        });

    }

    @Test
    public void retrieveCardPayments_forCMC() throws Exception {
        Date fromDate = new Date();
        MutableDateTime mFromDate = new MutableDateTime(fromDate);
        mFromDate.addDays(-1);
        Date toDate = new Date();
        MutableDateTime mToDate = new MutableDateTime(toDate);
        mToDate.addDays(2);

        List<PaymentFeeLink> result = cardPaymentService.search(mFromDate.toDate(), mToDate.toDate(), PaymentMethodType.CARD.getType(), "cmc", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPayments()).extracting("serviceType").contains("cmc");
    }

}
