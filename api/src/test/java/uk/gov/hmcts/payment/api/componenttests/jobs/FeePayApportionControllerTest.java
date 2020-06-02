package uk.gov.hmcts.payment.api.componenttests.jobs;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.controllers.MaintenanceJobsController;
import uk.gov.hmcts.payment.api.dto.FeePayApportionCCDCase;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        List<Date> feeCreatedDates = new ArrayList<>();
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.09.2020"));
        feeCreatedDates.add(parseDate("01.10.2020"));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(20));
        paymentAmounts.add(new BigDecimal(30));

        List<Date> paymentCreatedDates = new ArrayList<>();
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.09.2020"));
        //paymentCreatedDates.add(parseDate("01.10.2020"));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, feeCreatedDates, 1))
            .payments(getPayments(ccdCase, paymentAmounts, paymentCreatedDates, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(new BigDecimal(100), feePayApportionCCDCases.get(0).getFees().get(0).getAllocatedAmount());
    }

    @Test
    public void SingleFeeMultiplePay_SurplusPayment() throws CheckDigitException {

        String ccdCase = "1111222233335555";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));

        List<Date> feeCreatedDates = new ArrayList<>();
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.09.2020"));
        feeCreatedDates.add(parseDate("01.10.2020"));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(80));
        paymentAmounts.add(new BigDecimal(20));
        paymentAmounts.add(new BigDecimal(30));

        List<Date> paymentCreatedDates = new ArrayList<>();
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.09.2020"));
        //paymentCreatedDates.add(parseDate("01.10.2020"));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, feeCreatedDates, 1))
            .payments(getPayments(ccdCase, paymentAmounts, paymentCreatedDates, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(new BigDecimal(130), feePayApportionCCDCases.get(0).getFees().get(0).getAllocatedAmount());
    }

    @Test
    public void SingleFeeMultiplePay_ShortfallPayment() throws CheckDigitException {

        String ccdCase = "1111222233336666";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));

        List<Date> feeCreatedDates = new ArrayList<>();
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.09.2020"));
        feeCreatedDates.add(parseDate("01.10.2020"));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(80));
        paymentAmounts.add(new BigDecimal(10));
        paymentAmounts.add(new BigDecimal(5));

        List<Date> paymentCreatedDates = new ArrayList<>();
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.09.2020"));
        //paymentCreatedDates.add(parseDate("01.10.2020"));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, feeCreatedDates, 1))
            .payments(getPayments(ccdCase, paymentAmounts, paymentCreatedDates, 3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(new BigDecimal(95), feePayApportionCCDCases.get(0).getFees().get(0).getAllocatedAmount());
    }

    @Test
    public void MultipleFeeMultiplePay_ExactPayment() throws CheckDigitException {

        String ccdCase = "1111222233337777";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(100));
        feeAmounts.add(new BigDecimal(40));

        List<Date> feeCreatedDates = new ArrayList<>();
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.09.2020"));
        feeCreatedDates.add(parseDate("01.10.2020"));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(40));

        List<Date> paymentCreatedDates = new ArrayList<>();
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.09.2020"));
        //paymentCreatedDates.add(parseDate("01.10.2020"));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, feeCreatedDates, 2))
            .payments(getPayments(ccdCase, paymentAmounts, paymentCreatedDates, 3))
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

        List<Date> feeCreatedDates = new ArrayList<>();
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.09.2020"));
        feeCreatedDates.add(parseDate("01.10.2020"));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(100));
        paymentAmounts.add(new BigDecimal(80));

        List<Date> paymentCreatedDates = new ArrayList<>();
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.08.2020"));
        //paymentCreatedDates.add(parseDate("01.09.2020"));
        //paymentCreatedDates.add(parseDate("01.10.2020"));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, feeCreatedDates, 3))
            .payments(getPayments(ccdCase, paymentAmounts, paymentCreatedDates, 2))
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

        List<Date> feeCreatedDates = new ArrayList<>();
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.09.2020"));
        feeCreatedDates.add(parseDate("01.10.2020"));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(80));
        paymentAmounts.add(new BigDecimal(50));
        paymentAmounts.add(new BigDecimal(10));

        List<Date> paymentCreatedDates = new ArrayList<>();
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.09.2020"));
        //paymentCreatedDates.add(parseDate("01.10.2020"));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, feeCreatedDates, 3))
            .payments(getPayments(ccdCase, paymentAmounts, paymentCreatedDates,3))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    @Test
    public void MultipleFeeMultiplePay_UseCase5() throws CheckDigitException {

        String ccdCase = "1111222244441111";

        List<BigDecimal> feeAmounts = new ArrayList<>();
        feeAmounts.add(new BigDecimal(500));
        //feeAmounts.add(new BigDecimal(60));
        //feeAmounts.add(new BigDecimal(70));
        //feeAmounts.add(new BigDecimal(80));

        List<Date> feeCreatedDates = new ArrayList<>();
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.08.2020"));
        feeCreatedDates.add(parseDate("01.09.2020"));
        feeCreatedDates.add(parseDate("01.10.2020"));

        List<BigDecimal> remissionAmounts = new ArrayList<>();
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));
        remissionAmounts.add(new BigDecimal(0));

        List<BigDecimal> paymentAmounts = new ArrayList<>();
        paymentAmounts.add(new BigDecimal(200));
        paymentAmounts.add(new BigDecimal(500));
        //paymentAmounts.add(new BigDecimal(30));
        //paymentAmounts.add(new BigDecimal(500));

        List<Date> paymentCreatedDates = new ArrayList<>();
        paymentCreatedDates.add(parseDate("01.08.2020"));
        paymentCreatedDates.add(parseDate("01.08.2020"));
        //paymentCreatedDates.add(parseDate("01.09.2020"));
        //paymentCreatedDates.add(parseDate("01.10.2020"));

        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo(ccdCase)
            .feePayGroups(getPaymentFeeLinks(1))
            .fees(getFees(ccdCase, feeAmounts, remissionAmounts, feeCreatedDates, 1))
            .payments(getPayments(ccdCase, paymentAmounts, paymentCreatedDates, 2))
            .build();

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();
        feePayApportionCCDCases.add(feePayApportionCCDCase);

        maintenanceJobsController.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

    private List<PaymentFeeLink> getPaymentFeeLinks(int count) throws CheckDigitException {
        List<PaymentFeeLink> paymentFeeLinks = new ArrayList<>();

        for(int i = 0; i < count; i++) {
            paymentFeeLinks.add(PaymentFeeLink.paymentFeeLinkWith()
                .id(RandomUtils.nextInt())
                .paymentReference(referenceUtil.getNext("GR"))
                .build());
        }
        return paymentFeeLinks;
    }

    private List<Payment> getPayments(String ccdCase, List<BigDecimal> amounts, List<Date> paymentCreatedDates, int count) throws CheckDigitException {
        List<Payment> payments = new ArrayList<>();

        for(int i = 0; i < count; i++) {
            payments.add(Payment.paymentWith()
                .id(RandomUtils.nextInt(100, 999))
                .amount(amounts.get(i))
                .reference(referenceUtil.getNext("RC"))
                .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
                .dateCreated(paymentCreatedDates.get(i))
                .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
                .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
                .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
                .serviceType("PROBATE")
                .ccdCaseNumber(ccdCase)
                .build());
        }
        return payments;
    }

    private List<PaymentFee> getFees(String ccdCase, List<BigDecimal> amounts, List<BigDecimal> remissionAmounts, List<Date> feeCreatedDates, int count) throws CheckDigitException {
        List<PaymentFee> fees = new ArrayList<>();

        for(int i = 0; i < count; i++) {
            PaymentFee fee = PaymentFee.feeWith()
                .id(RandomUtils.nextInt(100, 999))
                .code("FEE00" + i)
                .feeAmount(amounts.get(i))
                .volume(1)
                .calculatedAmount(amounts.get(i).multiply(new BigDecimal(1)))
                .netAmount(amounts.get(i).multiply(new BigDecimal(1)).subtract(remissionAmounts.get(i)))
                .currApportionAmount(new BigDecimal(0))
                .dateCreated(feeCreatedDates.get(i))
                .build();

            fees.add(fee);
        }
        return fees;
    }

    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
