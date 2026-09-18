package uk.gov.hmcts.payment.api.mapper;

import nl.altindag.log.LogCaptor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Map;

import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponse;
import uk.gov.hmcts.payment.api.model.StatusHistory;

import static nl.altindag.log.LogCaptor.forClass;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.api.util.AccountStatus.*;

@RunWith(MockitoJUnitRunner.class)
public class PBAStatusErrorMapperTest {
    private static CreditAccountPaymentRequest creditAccountPaymentRequest;
    private static Payment payment;

    private static AccountDto activeAccountDetails;
    private static AccountDto activeAccountInsufficientDetails;
    private static AccountDto onHoldAccountDetails;
    private static AccountDto deletedAccountDetails;

    private static LogCaptor mockLOG;

    @BeforeClass
    public static void initiate(){
        mockLOG = forClass(PBAStatusErrorMapper.class);
        creditAccountPaymentRequest = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
                                        .amount(new BigDecimal("10.00"))
                                        .build();
        payment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();
        activeAccountDetails = AccountDto.accountDtoWith()
                            .accountName("account-name")
                            .availableBalance(new BigDecimal("100.00"))
                            .status(ACTIVE).build();
        activeAccountInsufficientDetails = AccountDto.accountDtoWith()
            .accountName("account-name")
            .availableBalance(new BigDecimal("9.00"))
            .build();
        onHoldAccountDetails = AccountDto.accountDtoWith()
            .accountName("account-name")
            .availableBalance(new BigDecimal("100.00"))
            .status(AccountStatus.ON_HOLD).build();
        deletedAccountDetails = AccountDto.accountDtoWith()
            .accountName("account-name")
            .availableBalance(new BigDecimal("100.00"))
            .status(AccountStatus.DELETED).build();
    }

    @AfterClass
    public static void closeDown() {
        if (mockLOG != null) {
            mockLOG = null;
        }
    }

    @After
    public void afterTest() {
        mockLOG.clearLogs();
    }

    @Test
    public void testSetPaymentStatusWithOnHold() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        String expected = "CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account is on hold!";
        pbaStatusErrorMapper.setPaymentStatus(creditAccountPaymentRequest, payment, onHoldAccountDetails);
        assertThat(mockLOG.getInfoLogs().get(0)).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ON_HOLD PaymentStatus : failed - Account is on hold!");
    }

    @Test
    public void testSetPaymentStatusWithActiveAndSufficientBalance() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        String expected = "CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance Sufficient!!!";
        pbaStatusErrorMapper.setPaymentStatus(creditAccountPaymentRequest,payment,activeAccountDetails);
        assertThat(mockLOG.getInfoLogs().get(0)).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ACTIVE PaymentStatus : success - Account Balance Sufficient!!!");
    }

    @Test
    public void testSetPaymentStatusWithDeletedStatus() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        String expected = "CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account is deleted!";
        pbaStatusErrorMapper.setPaymentStatus(creditAccountPaymentRequest,payment,deletedAccountDetails);
        assertThat(mockLOG.getInfoLogs().get(0)).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : DELETED PaymentStatus : failed - Account is deleted!");
    }

    @Test
    public void testSetPaymentStatusAndHistoriesSuccess() {
        PBAStatusErrorMapper mapper = new PBAStatusErrorMapper();
        PaymentAccountResponse resp = PaymentAccountResponse.paymentDtoWith()
            .status("success")
            .message(null)
            .build();

        Payment localPayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();

        mapper.setPaymentStatusAndHistories(creditAccountPaymentRequest, localPayment, resp);

        assertThat(localPayment.getPaymentStatus()).isNotNull();
        assertThat(localPayment.getPaymentStatus().getName()).isEqualTo("success");
        // success should not create status histories
        assertThat(localPayment.getStatusHistories()).isNull();
    }

    @Test
    public void testSetPaymentStatusAndHistoriesErrors() {
        PBAStatusErrorMapper mapper = new PBAStatusErrorMapper();

        for (Map.Entry<String, String> entry : PBAStatusErrorMapper.PBA_ERROR_CODE_MAP.entrySet()) {
            String message = entry.getKey();
            String expectedErrorCode = entry.getValue();

            Payment localPayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();

            PaymentAccountResponse resp = PaymentAccountResponse.paymentDtoWith()
                .status("error")
                .message(message)
                .build();

            mapper.setPaymentStatusAndHistories(creditAccountPaymentRequest, localPayment, resp);

            assertThat(localPayment.getPaymentStatus()).isNotNull();
            assertThat(localPayment.getPaymentStatus().getName()).isEqualTo("failed");

            assertThat(localPayment.getStatusHistories()).isNotNull();
            assertThat(localPayment.getStatusHistories()).hasSize(1);

            StatusHistory history = localPayment.getStatusHistories().get(0);
            assertThat(history.getStatus()).isEqualTo("failed");
            assertThat(history.getErrorCode()).isEqualTo(expectedErrorCode);

            assertThat(history.getMessage()).isEqualTo(message);
        }
    }

}
