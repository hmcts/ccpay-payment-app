package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.dto.FeePayApportionCCDCase;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RestController
@Api(tags = {"Maintenance Jobs"})
@SwaggerDefinition(tags = {@Tag(name = "MaintenanceJobsController", description = "Maintainance jobs REST API")})
public class MaintenanceJobsController {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceJobsController.class);

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    private final FeePayApportionService feePayApportionService;

    @Autowired
    private ReferenceUtil referenceUtil;

    @Autowired
    private TopicClientProxy topicClientProxy;

    @Autowired
    public MaintenanceJobsController(PaymentService<PaymentFeeLink, String> paymentService,
                                     DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                                     FeePayApportionService feePayApportionService) {
        this.paymentService = paymentService;
        this.delegatingPaymentService = delegatingPaymentService;
        this.feePayApportionService = feePayApportionService;
    }

    @ApiOperation(value = "Update payment status", notes = "Updates the payment status on all gov pay pending card payments")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Reports sent")
    })
    @PatchMapping(value = "/jobs/card-payments-status-update")
    @Transactional
    public void updatePaymentsStatus() {

        List<Reference> referenceList = paymentService.listInitiatedStatusPaymentsReferences();

        LOG.warn("Found {} references that require an status update", referenceList.size());

        /* We ask the topic client proxy to keep the reuse the connection to the service bus for the whole batch */
        if(topicClientProxy != null && !referenceList.isEmpty()) {
            topicClientProxy.setKeepClientAlive(true);
        }

        long count = referenceList
            .stream()
            .map(Reference::getReference)
            .map(delegatingPaymentService::retrieveWithCallBack)
            .filter(p -> p != null && p.getPayments() != null && p.getPayments().get(0) != null && p.getPayments().get(0).getStatus() != null)
            .count();

        LOG.warn("{} payment references were successfully updated", count);

        if(topicClientProxy != null) {
            topicClientProxy.setKeepClientAlive(false);
            topicClientProxy.close();
        }

    }

    @ApiOperation(value = "Auto Apportionment of Existing Fees & Payments", notes = "Auto Apportionment of Existing Fees & Payments for Online, Telephony & Offline channels")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Auto Apportionment of Existing Fees & Payments Done")
    })
    @PatchMapping(value = "/jobs/fee-pay-auto-apportion-historical")
    @Transactional
    public void feePayAutoApportion() {

        List<FeePayApportionCCDCase> feePayApportionCCDCases = feePayApportionService.findAllHistoricalCases();
        feePayApportionCCDCases.stream().forEach(feePayApportionCCDCase -> {
            System.out.println("Apportion Case ----------> " + feePayApportionCCDCase.getCcdCaseNo());
            System.out.println("FeePayGroups ----------> " + feePayApportionCCDCase.getFeePayGroups().size());
            System.out.println("Fees ----------> " + feePayApportionCCDCase.getFees().size());
            System.out.println("Payments ----------> " + feePayApportionCCDCase.getPayments().size());
            if(feePayApportionCCDCase.getCcdCaseNo().length() == 16) {
                System.out.println("Apportion Started ----------> " + feePayApportionCCDCase.getCcdCaseNo());
                feePayApportionService.processFeePayApportion(feePayApportionCCDCase);
                System.out.println("Apportion Completed ----------> " + feePayApportionCCDCase.getCcdCaseNo());
            }
        });
    }

    @ApiOperation(value = "Auto Apportionment of Existing Fees & Payments", notes = "Auto Apportionment of Existing Fees & Payments for Online, Telephony & Offline channels")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Auto Apportionment of Existing Fees & Payments Done")
    })
    @PatchMapping(value = "/jobs/fee-pay-auto-apportion-local")
    @Transactional
    public void feePayAutoApportionLocal() throws CheckDigitException {

        SingleFeeMultiplePay_ExactPayment();
        SingleFeeMultiplePay_SurplusPayment();
        SingleFeeMultiplePay_ShortfallPayment();
        MultipleFeeMultiplePay_ExactPayment();
        MultipleFeeMultiplePay_SurplusPayment();
        MultipleFeeMultiplePay_ShortfallPayment();
        MultipleFeeMultiplePay_UseCase5();
    }

    public List<FeePayApportionCCDCase> processFeePayApportion(List<FeePayApportionCCDCase> feePayApportionCCDCases) {
        for(FeePayApportionCCDCase feePayApportionCCDCase : feePayApportionCCDCases) {
            System.out.println("Apportion Case ----------> " + feePayApportionCCDCase.getCcdCaseNo());
            System.out.println("FeePayGroups ----------> " + feePayApportionCCDCase.getFeePayGroups().size());
            System.out.println("Fees ----------> " + feePayApportionCCDCase.getFees().size());
            System.out.println("Payments ----------> " + feePayApportionCCDCase.getPayments().size());

            //Sorting Fees for a Case in Date Created Ascending order
            feePayApportionCCDCase.setFees(feePayApportionCCDCase.getFees().stream()
                .sorted(Comparator.comparing(PaymentFee::getDateCreated))
                .collect(Collectors.toList()));

            //Sorting Payments for a Case in Date Created Ascending order
            feePayApportionCCDCase.setPayments(feePayApportionCCDCase.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getDateCreated))
                .collect(Collectors.toList()));

            if(feePayApportionCCDCase.getCcdCaseNo().length() == 16) {
                System.out.println("Apportion Started -------------------------------->");
                feePayApportionService.processFeePayApportion(feePayApportionCCDCase);
                System.out.println("Apportion Completed -------------------------------->");
            }
        }
        return feePayApportionCCDCases;
    }

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

        this.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

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

        this.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

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

        this.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

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

        this.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

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

        this.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

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

        this.processFeePayApportion(feePayApportionCCDCases);

        assertEquals(1, 1);
    }

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

        this.processFeePayApportion(feePayApportionCCDCases);

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
