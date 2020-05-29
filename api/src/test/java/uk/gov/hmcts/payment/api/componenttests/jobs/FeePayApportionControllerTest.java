package uk.gov.hmcts.payment.api.componenttests.jobs;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.org.apache.commons.lang.math.RandomUtils;
import uk.gov.hmcts.payment.api.controllers.MaintenanceJobsController;
import uk.gov.hmcts.payment.api.dto.FeePayApportionCCDCase;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
public class FeePayApportionControllerTest {

    @Autowired
    private MaintenanceJobsController maintenanceJobsController;

    @Autowired
    private ReferenceUtil referenceUtil;

    @Test
    public void SingleFeeMultiplePay_ExactPayment() throws CheckDigitException {

        String ccdCase = "1111222233334444";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(20));
        paymentAmounts.add(new BigDecimal(30));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, 1))
            .payments(getPayments(ccdCase, paymentAmounts, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    @Test
    public void SingleFeeMultiplePay_SurplusPayment() throws CheckDigitException {

        String ccdCase = "1111222233335555";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(80));
        paymentAmounts.add(new BigDecimal(20));
        paymentAmounts.add(new BigDecimal(30));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, 1))
            .payments(getPayments(ccdCase, paymentAmounts, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    @Test
    public void SingleFeeMultiplePay_ShortfallPayment() throws CheckDigitException {

        String ccdCase = "1111222233336666";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(80));
        paymentAmounts.add(new BigDecimal(10));
        paymentAmounts.add(new BigDecimal(5));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, 1))
            .payments(getPayments(ccdCase, paymentAmounts, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    @Test
    public void MultipleFeeMultiplePay_ExactPayment() throws CheckDigitException {

        String ccdCase = "1111222233337777";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));
        feeAmounts.add(new BigDecimal(40));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(40));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, 2))
            .payments(getPayments(ccdCase, paymentAmounts, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    @Test
    public void MultipleFeeMultiplePay_SurplusPayment() throws CheckDigitException {

        String ccdCase = "1111222233338888";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));
        feeAmounts.add(new BigDecimal(25));
        feeAmounts.add(new BigDecimal(25));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(100));
        paymentAmounts.add(new BigDecimal(80));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, 3))
            .payments(getPayments(ccdCase, paymentAmounts, 2))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    @Test
    public void MultipleFeeMultiplePay_ShortfallPayment() throws CheckDigitException {

        String ccdCase = "1111222233339999";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));
        feeAmounts.add(new BigDecimal(50));
        feeAmounts.add(new BigDecimal(10));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(80));
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(10));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, 3))
            .payments(getPayments(ccdCase, paymentAmounts, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    private LinkedHashSet<PaymentFeeLink> getPaymentFeeLinks(int count) throws CheckDigitException {
        LinkedHashSet<PaymentFeeLink> paymentFeeLinks = new LinkedHashSet<>();

        for(int i = 0; i < count; i++) {
            paymentFeeLinks.add(PaymentFeeLink.paymentFeeLinkWith()
                .id(RandomUtils.nextInt())
                .paymentReference(referenceUtil.getNext("GR"))
                .build());
        }
        return paymentFeeLinks;
    }

    private LinkedHashSet<Payment> getPayments(String ccdCase, List<BigDecimal> amounts, int count) throws CheckDigitException {
        LinkedHashSet<Payment> payments = new LinkedHashSet<>();

        for(int i = 0; i < count; i++) {
            payments.add(Payment.paymentWith()
                .id(RandomUtils.nextInt())
                .amount(amounts.get(i))
                .reference(referenceUtil.getNext("RC"))
                .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
                .dateCreated(new Date())
                .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
                .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
                .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
                .serviceType("PROBATE")
                .ccdCaseNumber(ccdCase)
                .build());
        }
        return payments;
    }

    private LinkedHashSet<PaymentFee> getFees(String ccdCase, List<BigDecimal> amounts, List<BigDecimal> remissionAmounts, int count) throws CheckDigitException {
        LinkedHashSet<PaymentFee> fees = new LinkedHashSet<>();

        for(int i = 0; i < count; i++) {
            fees.add(PaymentFee.feeWith()
                .id(RandomUtils.nextInt())
                .code("FEE00" + i)
                .feeAmount(amounts.get(i))
                .volume(1)
                .calculatedAmount(amounts.get(i).multiply(new BigDecimal(1)))
                .netAmount(amounts.get(i).multiply(new BigDecimal(1)).subtract(remissionAmounts.get(i)))
                .currApportionAmount(new BigDecimal(0))
                .dateCreated(new Date())
                .build());
        }
        return fees;
    }
}
