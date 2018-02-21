package uk.gov.hmcts.payment.api.componenttests;

import org.joda.time.MutableDateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.v1.componenttests.ComponentTestBase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.payment.api.model.Fee.feeWith;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;

public class CreditAccountPaymentServiceTest extends ComponentTestBase{

    @Before
    public void setUp() {

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .payments(getCreditAccountPaymentsData())
            .fees(getFeesData())
            .build();

        PaymentFeeLink result = paymentFeeLinkRepository.save(paymentFeeLink);
        System.out.println(result.getPayments().size());
    }

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
            g.getPayments().stream().forEach(p -> {
                assertEquals(p.getPaymentProvider().getName(), "middle office provider");
            });
        });

    }

    private List<Payment> getCreditAccountPaymentsData() {
        List<Payment> payments = new ArrayList<>();
        payments.add(paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1")
            .returnUrl("returnUrl1")
            .paymentProvider(PaymentProvider.paymentProviderWith().name("middle office provider").build())
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(20000).movePointRight(2)).reference("reference2").description("desc2")
            .returnUrl("returnUrl2")
            .paymentProvider(PaymentProvider.paymentProviderWith().name("middle office provider").build())
            .ccdCaseNumber("ccdCaseNo2").caseReference("caseRef2").serviceType("divorce").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(30000).movePointRight(2)).reference("reference3").description("desc3")
            .returnUrl("returnUrl3")
            .paymentProvider(PaymentProvider.paymentProviderWith().name("middle office provider").build())
            .ccdCaseNumber("ccdCaseNo3").caseReference("caseRef3").serviceType("probate").currency("GBP").build());

        return payments;
    }

    private List<Fee> getFeesData() {
        List<Fee> fees = new ArrayList<>();
        fees.add(feeWith().code("X0011").version("1").build());
        fees.add(feeWith().code("X0022").version("2").build());
        fees.add(feeWith().code("X0033").version("3").build());
        fees.add(feeWith().code("X0044").version("4").build());

        return fees;
    }
}
