package uk.gov.hmcts.payment.api.componenttests;

import com.sun.org.apache.bcel.internal.generic.LUSHR;
import org.joda.time.MutableDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.v1.componenttests.ComponentTestBase;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.payment.api.model.Fee.feeWith;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;

public class CardPaymentServiceTest extends ComponentTestBase {

    @Before
    public void setUp() {

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .payments(getPaymentsData())
            .fees(getFeesData())
            .build();

        PaymentFeeLink result = paymentFeeLinkRepository.save(paymentFeeLink);
        System.out.println(result.getPayments().size());
    }

    public void retievePaymentGroups() throws Exception {
        Date fromDate = new Date();
        MutableDateTime mFromDate = new MutableDateTime(fromDate);
        mFromDate.addDays(-1);
        Date toDate = new Date();
        MutableDateTime mToDate = new MutableDateTime(toDate);
        mToDate.addDays(2);

        List<PaymentFeeLink> lst = paymentFeeLinkRepository.findAll(findCardPaymentsByBetweenDates(mFromDate.toDate(), mToDate.toDate()));
        System.out.println("Tarun Tarun: " + lst.size());
    }

    private static Specification findCardPaymentsByBetweenDates(Date fromDate, Date toDate) {
        return Specifications
            .where(isEquals(PaymentProvider.paymentProviderWith().name("gov pay").build()));
            //.where(isBetween(fromDate, toDate));
    }

    private static Specification isEquals(PaymentProvider paymentProvider) {
        return ((root, query, cb) -> {
            Join<PaymentFeeLink, Payment> paymentJoin = root.join("payments", JoinType.LEFT);
            return cb.equal(paymentJoin.get("paymentProvider").get("name"), paymentProvider.getName());
        });

    }

    private static Specification isBetween(Date startDate, Date endDate) {

        return ((root, query, cb) -> cb.between(root.get("dateCreated"), startDate, endDate));
    }


    public void retireveCardPaymentsTest2() throws Exception {
        List<PaymentFeeLink> lst = new ArrayList<>();
        paymentFeeLinkRepository.findAll().forEach(lst::add);
        System.out.println("Tarun tarun: " + lst.size());

        Date fromDate = new Date();
        MutableDateTime mFromDate = new MutableDateTime(fromDate);
        mFromDate.addDays(-1);
        Date toDate = new Date();
        MutableDateTime mToDate = new MutableDateTime(toDate);
        mToDate.addDays(2);

        System.out.println("FromDate: " + mFromDate.toDate() + " ToDate: " + mToDate.toDate());
        List<PaymentFeeLink> result = cardPaymentService.search(mFromDate.toDate(), mToDate.toDate());

        System.out.println("Tarun:::: " + result.size());

        result.stream().forEach(g -> {
            assertEquals(g.getPayments().size(), 3);
            assertEquals(g.getFees().size(), 4);
            g.getPayments().stream().forEach(p -> {
                System.out.println("payment provider: " + p.getReference());
            });
        });

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
