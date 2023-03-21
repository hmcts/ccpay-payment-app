package uk.gov.hmcts.payment.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.dto.FeePayApportionCCDCase;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.v1.componenttests.TestUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;
import static uk.gov.hmcts.payment.api.model.PaymentFee.feeWith;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public class FeePayApportionServiceTest extends TestUtil {

    @Autowired
    protected FeePayApportionService feePayApportionService;
    Payment payment = paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1").returnUrl("returnUrl1")
        .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP")
        .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
            .externalStatus("created")
            .status("Initiated")
            .build()))
        .build();

    PaymentFee fee = feeWith().code("FEE0111").version("1").netAmount(BigDecimal.valueOf(455.00)).build();
    List<Remission> remissions = Arrays.asList(Remission.remissionWith().remissionReference("AA").
            ccdCaseNumber("ccdCaseNo1").build());
    PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
        .payments(Arrays.asList(payment)).remissions(remissions)
        .fees(Arrays.asList(fee))
        .build();


    private PaymentsDataUtil paymentsDataUtil;
    @MockBean
    private FeePayApportionRepository feePayApportionRepository;
    @MockBean
    private PaymentFeeRepository paymentFeeRepository;

    @Test(expected = PaymentException.class)
    public void updateFeeAmountDueTest() {
        paymentFeeLinkRepository.save(paymentFeeLink);
        Mockito.when(feePayApportionRepository.findByPaymentId(Mockito.any())).thenThrow(new RuntimeException("DB Exception"));
        feePayApportionService.updateFeeAmountDue(paymentFeeLink.getPayments().get(0));
    }

    @Test(expected = PaymentException.class)
    public void processApportionTest() {
        paymentFeeLinkRepository.save(paymentFeeLink);
        Mockito.when(paymentFeeRepository.findByCcdCaseNumber(Mockito.any())).thenThrow(new RuntimeException("DB Exception"));
        feePayApportionService.processApportion(paymentFeeLink.getPayments().get(0));
    }

    @Test
    public void testGetFeeCalculatedNetAmount() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        Date date = dateFormat.parse("2019-04-05 11:40:30");
        long time = date.getTime();
        fee.setDateCreated(new Timestamp(time));
        PaymentFeeLink paymentFeeLink1 = PaymentFeeLink.paymentFeeLinkWith().paymentReference("00000005")
            .payments(Arrays.asList(payment)).remissions(remissions)
            .fees(Arrays.asList(fee))
            .build();
        Optional<List<PaymentFee>> paymentFees = Optional.of(Arrays.asList(PaymentFee.feeWith().paymentLink
            (paymentFeeLink1).build()));
        payment.setPaymentLink(paymentFeeLink1);
        when(paymentFeeRepository.findByPaymentLinkId(anyInt())).thenReturn(paymentFees);
        paymentFeeLinkRepository.save(paymentFeeLink1);
        Mockito.when(paymentFeeRepository.findByCcdCaseNumber(Mockito.any())).thenThrow(new RuntimeException("DB Exception"));
        feePayApportionService.processApportion(paymentFeeLink1.getPayments().get(0));
    }

    @Test(expected = PaymentException.class)
    public void processFeePayApportionTest() {
        PaymentFeeLink paymentFeeLink1 = paymentFeeLinkRepository.save(paymentFeeLink);
        FeePayApportionCCDCase feePayApportionCCDCase = FeePayApportionCCDCase.feePayApportionCCDCaseWith()
            .ccdCaseNo("1234234534565678")
            .feePayGroups(Collections.singletonList(paymentFeeLink1))
            .payments(Collections.singletonList(paymentFeeLink1.getPayments().get(0)))
            .build();
        feePayApportionService.processFeePayApportion(feePayApportionCCDCase);
    }
}
