package uk.gov.hmcts.payment.api.componenttests.util;

import uk.gov.hmcts.payment.api.model.Fee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.payment.api.model.Fee.feeWith;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;

public class PaymentsDataUtil {

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

    public List<Fee> getFeesData() {
        List<Fee> fees = new ArrayList<>();
        fees.add(feeWith().code("X0011").version("1").build());
        fees.add(feeWith().code("X0022").version("2").build());
        fees.add(feeWith().code("X0033").version("3").build());
        fees.add(feeWith().code("X0044").version("4").build());

        return fees;
    }
}
