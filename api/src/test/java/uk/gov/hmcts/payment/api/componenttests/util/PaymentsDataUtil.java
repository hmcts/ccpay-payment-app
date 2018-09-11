package uk.gov.hmcts.payment.api.componenttests.util;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static uk.gov.hmcts.payment.api.model.PaymentFee.feeWith;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

public class PaymentsDataUtil {

    @Autowired
    protected PaymentDbBackdoor db;

    private static final String USER_ID = "user-id";

    public List<Payment> getCreditAccountPaymentsData() {
        List<Payment> payments = new ArrayList<>();
        payments.add(paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1")
            .returnUrl("returnUrl1")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(20000).movePointRight(2)).reference("reference2").description("desc2")
            .returnUrl("returnUrl2")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .ccdCaseNumber("ccdCaseNo2").caseReference("caseRef2").serviceType("divorce").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(30000).movePointRight(2)).reference("reference3").description("desc3")
            .returnUrl("returnUrl3")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .ccdCaseNumber("ccdCaseNo3").caseReference("caseRef3").serviceType("probate").currency("GBP").build());

        return payments;
    }

    public List<Payment> getCardPaymentsData() {
        List<Payment> payments = new ArrayList<>();
        payments.add(paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1")
            .returnUrl("returnUrl1")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(20000).movePointRight(2)).reference("reference2").description("desc2")
            .returnUrl("returnUrl2")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .ccdCaseNumber("ccdCaseNo2").caseReference("caseRef2").serviceType("divorce").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(30000).movePointRight(2)).reference("reference3").description("desc3")
            .returnUrl("returnUrl3")
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .ccdCaseNumber("ccdCaseNo3").caseReference("caseRef3").serviceType("probate").currency("GBP").build());

        return payments;
    }

    public List<PaymentFee> getFeesData() {
        List<PaymentFee> fees = new ArrayList<>();
        fees.add(feeWith().code("X0011").version("1").build());
        fees.add(feeWith().code("X0022").version("2").build());
        fees.add(feeWith().code("X0033").version("3").build());
        fees.add(feeWith().code("X0044").version("4").build());

        return fees;
    }

    private Payment getExampleCardPaymentWithNumber(String number) {
        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();

        return Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Test payments statuses for " + number)
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0" + number)
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v" + number)
            .reference("RC-1519-9028-2432-000" + number)
            .statusHistories(Arrays.asList(statusHistory))
            .build();
    }

    public Payment populateCardPaymentWithSpecifiedCreationDateToDb(String number) {
        Payment payment = getExampleCardPaymentWithNumber(number);

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE000" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000000" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
        return payment;
    }

    public void populateCardPaymentToDb(String number) throws Exception {
        //Create a payment in db
        Payment payment = getExampleCardPaymentWithNumber(number);

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE000" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000000" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
    }


    public void populateCreditAccountPaymentToDb(String number) throws Exception {
        //Create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Description" + number)
            .serviceType("Probate")
            .currency("GBP")
            .siteId("AA0" + number)
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-000" + number)
            .build();

        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("FEE000" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-0000000000" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

    }
}
