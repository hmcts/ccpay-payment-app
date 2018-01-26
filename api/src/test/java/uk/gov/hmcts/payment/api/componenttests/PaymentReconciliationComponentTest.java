package uk.gov.hmcts.payment.api.componenttests;

import org.joda.time.MutableDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.componenttests.ComponentTestBase;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.payment.api.model.Fee.feeWith;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

public class PaymentReconciliationComponentTest extends ComponentTestBase {

    @Test
    public void testSaveOfSinglePaymentWithSingleFee() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String paymentRef1 = UUID.randomUUID().toString();
        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(paymentFeeLinkWith().paymentReference(paymentRef1)
            .payments(getPaymentsData())
            .fees(getFeesData())
            .build());

        String paymentRef2 = UUID.randomUUID().toString();
        paymentFeeLinkRepository.save(paymentFeeLinkWith().paymentReference(paymentRef2)
            .payments(Arrays.asList(getPaymentsData().get(1)))
            .fees(Arrays.asList(getFeesData().get(0)))
            .build());



        Date fromDate = new Date();
        MutableDateTime mFromDate = new MutableDateTime(fromDate);
        mFromDate.addDays(-1);
        Date toDate = new Date();
        MutableDateTime mToDate = new MutableDateTime(toDate);

        List<PaymentFeeLink> paymentFeeLinks = paymentFeeLinkRepository.findAll(findByDatesBetween(mFromDate.toDate(), mToDate.toDate()));

        assertNotNull(paymentFeeLink);
        assertEquals(paymentFeeLinks.size(), 2);
    }

    private static Specification findByDatesBetween(Date fromDate, Date toDate) {
        return Specifications
            .where(isBetween(fromDate, toDate));
    }

    private static Specification isBetween(Date startDate, Date endDate) {

        return ((root, query, cb) -> cb.between(root.get("dateCreated"), startDate, endDate));
    }

    private List<Payment> getPaymentsData() {
        List<Payment> payments = new ArrayList<>();
        payments.add(paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1").returnUrl("returnUrl1")
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(20000).movePointRight(2)).reference("reference2").description("desc2").returnUrl("returnUrl2")
            .ccdCaseNumber("ccdCaseNo2").caseReference("caseRef2").serviceType("divorce").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(30000).movePointRight(2)).reference("reference3").description("desc3").returnUrl("returnUrl3")
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
