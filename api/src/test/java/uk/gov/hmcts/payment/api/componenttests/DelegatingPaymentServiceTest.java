package uk.gov.hmcts.payment.api.componenttests;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.componenttests.TestUtil;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DirtiesContext(classMode= DirtiesContext.ClassMode.BEFORE_CLASS)
public class DelegatingPaymentServiceTest extends TestUtil {
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
    public void retrieveCardPayments_forBetweenDates_WhereProviderIsGovPayTest() {
        Date fromDate = new Date();
        DateTime mFromDate = new DateTime(fromDate);
        mFromDate.minusDays(1);
        Date toDate = new Date();
        DateTime mToDate = new DateTime(toDate);
        mToDate.plusDays(2);

        List<PaymentFeeLink> result = cardPaymentService.search(

            PaymentSearchCriteria
                .searchCriteriaWith()
                .startDate(mFromDate.toDate())
                .endDate(mToDate.toDate())
                .paymentMethod(PaymentMethodType.CARD.getType())
                .build()

        );

        assertNotNull(result);
        result.stream().forEach(g -> {
            assertEquals(g.getPayments().size(), 3);
            g.getPayments().stream().forEach(p -> {
                assertEquals(p.getPaymentMethod().getName(), "card");
            });
        });

    }

    @Test
    public void retrieveCardPayments_forCMC() {

        List<PaymentFeeLink> result = cardPaymentService.search(
                PaymentSearchCriteria
                        .searchCriteriaWith()
                        .paymentMethod(PaymentMethodType.CARD.getType())
                        .serviceType("cmc")
                        .build()
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPayments()).extracting("serviceType").contains("cmc");
    }

}
