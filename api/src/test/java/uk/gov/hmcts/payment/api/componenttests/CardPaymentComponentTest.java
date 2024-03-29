package uk.gov.hmcts.payment.api.componenttests;


import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.TestUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@DirtiesContext(classMode= DirtiesContext.ClassMode.BEFORE_CLASS)
@Slf4j
public class CardPaymentComponentTest extends TestUtil {

    @Autowired
    protected PaymentFeeRepository paymentFeeRepository;

    @Test
    public void testSaveOfSinglePaymentWithSingleFee() {
        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(paymentFeeLinkWith().paymentReference("00000001")
            .payments(Arrays.asList(getPaymentsData().get(0)))
            .fees(Arrays.asList(PaymentsDataUtil.getFeesData().get(2)))
            .build());

        assertNotNull(paymentFeeLink);
        assertEquals(paymentFeeLink.getPayments().size(), 1);
        assertEquals(paymentFeeLink.getFees().size(), 1);
        assertEquals(paymentFeeLink.getPayments().get(0).getAmount(), new BigDecimal(1000000));
        assertEquals(paymentFeeLink.getFees().get(0).getCode(), "X0033");
        paymentFeeLink.getPayments().get(0).getStatusHistories().stream().forEach(h -> {
            assertEquals(h.getExternalStatus(), "created");
            assertEquals(h.getStatus(), "Initiated");
        });
    }


    @Test
    public void testSaveOfSinglePaymentWithMultipleFees() {
        PaymentFeeLink paymentFeeLink1 = PaymentFeeLink.paymentFeeLinkWith()
                .payments(Arrays.asList(getPaymentsData().get(0)))
                .fees(PaymentsDataUtil.getFeesData())
                .paymentReference("00000002")
                .build();
        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(paymentFeeLink1);

        assertNotNull(paymentFeeLink.getId());
        assertNotNull(paymentFeeLink.getPayments());
        assertNotNull(paymentFeeLink.getFees());
        assertEquals(paymentFeeLink.getPayments().size(), 1);
        assertEquals(paymentFeeLink.getPaymentReference(), "00000002");
        assertEquals(paymentFeeLink.getPayments().get(0).getReference(), "reference1");
        assertEquals(paymentFeeLink.getFees().size(), 4);
    }

    @Test
    public void testSaveOfMutliplePaymentsWithMutlipleFees() {

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(paymentFeeLinkWith().paymentReference("00000003")
            .payments(getPaymentsData())
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        assertNotNull(paymentFeeLink.getId());
        assertNotNull(paymentFeeLink.getPayments().get(0).getId());
        assertNotNull(paymentFeeLink.getFees().get(2).getId());
        assertNotNull(paymentFeeLink.getPayments().get(0).getId());
        assertEquals(paymentFeeLink.getPaymentReference(), "00000003");
        assertEquals(paymentFeeLink.getPayments().size(), 3);
        assertEquals(paymentFeeLink.getFees().size(), 4);
    }


    @Test
    public void testRetrieveCardPaymentWithPaymentReference() throws Exception {

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000004")
            .payments(Arrays.asList(getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        PaymentFeeLink foundPayment = paymentFeeLinkRepository.findByPaymentReference("00000004").orElseThrow(PaymentNotFoundException::new);

        Payment payment = foundPayment.getPayments().get(0);
        assertNotNull(payment.getId());
        assertEquals(payment.getAmount(), new BigDecimal(3000000));
    }

    @Test
    public void testRetrieveCardPaymentWithDuplicatePaymentReferenceUsingCcdCaseNumber() throws Exception {

        PaymentFeeLink paymentFeeLinkSingle = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000004")
            .ccdCaseNumber("1282584933728638")
            .payments(Arrays.asList(getPaymentsData().get(1)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        PaymentFeeLink paymentFeeLinkDup1of2 = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .ccdCaseNumber("1282584933728640")
            .payments(Arrays.asList(getPaymentsData().get(1)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        PaymentFeeLink paymentFeeLinkDup2of2 = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .ccdCaseNumber("1282584933728642")
            .payments(Arrays.asList(getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        PaymentFeeLink foundPayment1 = findByPaymentReferenceAndOrCcdCaseNumber("00000004", null);
        PaymentFeeLink foundPayment2 = findByPaymentReferenceAndOrCcdCaseNumber("00000005", "1282584933728640");
        PaymentFeeLink foundPayment3 = findByPaymentReferenceAndOrCcdCaseNumber("00000005", "1282584933728642");

        Payment payment1 = foundPayment1.getPayments().get(0);
        assertNotNull(payment1.getId());
        assertEquals(payment1.getAmount(), new BigDecimal(2000000));

        Payment payment2 = foundPayment2.getPayments().get(0);
        assertNotNull(payment2.getId());
        assertEquals(payment2.getAmount(), new BigDecimal(2000000));

        Payment payment3 = foundPayment3.getPayments().get(0);
        assertNotNull(payment3.getId());
        assertEquals(payment3.getAmount(), new BigDecimal(3000000));
    }

    @Test
    public void testRetrieveCardPaymentWithDuplicatePaymentReferenceUsingFeeId() throws Exception {

        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000006")
            .ccdCaseNumber("ccdCaseNo2")
            .payments(Arrays.asList(getPaymentsData().get(1)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        PaymentFeeLink paymentFeeLink2 = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000006")
            .ccdCaseNumber("ccdCaseNo3")
            .payments(Arrays.asList(getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData2())
            .build());

        List<PaymentFee> fees = paymentFeeRepository.findAll();

        PaymentFeeLink foundPayment1 = findByPaymentReferenceAndOrFeeId("00000006", fees.get(1).getId());
        PaymentFeeLink foundPayment2 = findByPaymentReferenceAndOrFeeId("00000006", fees.get(5).getId());

        Payment payment1 = foundPayment1.getPayments().get(0);
        assertNotNull(payment1.getId());
        assertEquals(payment1.getCcdCaseNumber(), "ccdCaseNo2");
        assertEquals(payment1.getAmount(), new BigDecimal(2000000));

        Payment payment2 = foundPayment2.getPayments().get(0);
        assertNotNull(payment2.getId());
        assertEquals(payment2.getCcdCaseNumber(), "ccdCaseNo3");
        assertEquals(payment2.getAmount(), new BigDecimal(3000000));
    }

    @Test(expected = PaymentNotFoundException.class)
    public void testRetrieveCardPaymentWithNonExistingPaymentReferenceShouldThrowException() throws Exception {
        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000007")
            .payments(Arrays.asList(getPaymentsData().get(2)))
            .fees(PaymentsDataUtil.getFeesData())
            .build());

        PaymentFeeLink foundPayment = paymentFeeLinkRepository.findByPaymentReference("00000008").orElseThrow(PaymentNotFoundException::new);
    }

    private PaymentFeeLink findByPaymentReferenceAndOrCcdCaseNumber(String paymentGroupReference, String ccdCaseNumber) {
        Optional<PaymentFeeLink> paymentFeeLink;
        try {
            paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference);
        } catch (IncorrectResultSizeDataAccessException error) {
            paymentFeeLink = paymentFeeLinkRepository.findByPaymentReferenceAndCcdCaseNumber(paymentGroupReference, ccdCaseNumber);
        }
        return paymentFeeLink.orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + " ERROR"));
    }

    private PaymentFeeLink findByPaymentReferenceAndOrFeeId(String paymentGroupReference, Integer feeId) {
        Optional<PaymentFeeLink> paymentFeeLink;
        try {
            paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference);
        } catch (IncorrectResultSizeDataAccessException error) {
            paymentFeeLink = paymentFeeLinkRepository.findByPaymentReferenceAndFeeId(paymentGroupReference, feeId);
        }
        return paymentFeeLink.orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + " ERROR"));
    }

    public static List<Payment> getPaymentsData() {
        List<Payment> payments = new ArrayList<>();
        payments.add(paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1").returnUrl("https://www.moneyclaims.service.gov.uk")
            .paymentStatus(PaymentStatus.CREATED)
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP")
            .serviceCallbackUrl("http://google.com")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .externalStatus("created")
                .status("Initiated")
                .build()))
            .build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(20000).movePointRight(2)).reference("reference2").description("desc2").returnUrl("https://www.moneyclaims.service.gov.uk")
            .paymentStatus(PaymentStatus.CREATED)
            .serviceCallbackUrl("http://google.com")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .externalStatus("created")
                .status("Initiated")
                .build()))
            .ccdCaseNumber("ccdCaseNo2").caseReference("caseRef2").serviceType("divorce").currency("GBP").build());
        payments.add(paymentWith().amount(BigDecimal.valueOf(30000).movePointRight(2)).reference("reference3").description("desc3").returnUrl("https://www.moneyclaims.service.gov.uk")
            .paymentStatus(PaymentStatus.CREATED)
            .serviceCallbackUrl("http://google.com")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .externalStatus("created")
                .status("Initiated")
                .build()))
            .ccdCaseNumber("ccdCaseNo3").caseReference("caseRef3").serviceType("probate").currency("GBP").build());
        return payments;
    }
}
