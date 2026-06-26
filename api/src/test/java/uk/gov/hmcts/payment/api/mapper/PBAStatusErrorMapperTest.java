package uk.gov.hmcts.payment.api.mapper;

import net.minidev.json.JSONObject;
import nl.altindag.log.LogCaptor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;

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
        JSONObject responseBody = new JSONObject();
        responseBody.put("error_code", "4");
        responseBody.put("description", "Account is not active");
        ResponseEntity<JSONObject> response = new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN);
        pbaStatusErrorMapper.setLiberataPaymentStatus(creditAccountPaymentRequest, payment, onHoldAccountDetails, response);
        assertThat(mockLOG.getInfoLogs().getFirst()).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ON_HOLD PaymentStatus : failed - Account is not active");
    }

    @Test
    public void testSetPaymentStatusWithExceededCreditLimit() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        JSONObject responseBody = new JSONObject();
        responseBody.put("error_code", "1");
        responseBody.put("description", "Exceeded credit limit.");
        ResponseEntity<JSONObject> response = new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN);
        pbaStatusErrorMapper.setLiberataPaymentStatus(creditAccountPaymentRequest, payment, activeAccountDetails, response);
        assertThat(mockLOG.getInfoLogs().getFirst()).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ACTIVE PaymentStatus : failed - Exceeded credit limit.");
    }

    @Test
    public void testSetPaymentStatusWithActiveAndSufficientBalance() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        String expected = "CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance Sufficient!!!";
        JSONObject responseBody = new JSONObject();
        ResponseEntity<JSONObject> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        pbaStatusErrorMapper.setLiberataPaymentStatus(creditAccountPaymentRequest, payment, activeAccountDetails, response);
        assertThat(mockLOG.getInfoLogs().getFirst()).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ACTIVE PaymentStatus : success - Account Balance Sufficient!!!");
    }

    @Test
    public void testSetPaymentStatusWithDeletedStatus() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        JSONObject responseBody = new JSONObject();
        responseBody.put("error_code", "2");
        responseBody.put("description", "Account not found.");
        ResponseEntity<JSONObject> response = new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN);
        pbaStatusErrorMapper.setLiberataPaymentStatus(creditAccountPaymentRequest,payment,deletedAccountDetails, response);
        assertThat(mockLOG.getInfoLogs().getFirst()).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : DELETED PaymentStatus : failed - Account not found.");
    }

    @Test
    public void testSetPaymentStatusWithNoSpecifiedErrorCodeOrDescription() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        JSONObject responseBody = new JSONObject();
        ResponseEntity<JSONObject> response = new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN);
        pbaStatusErrorMapper.setLiberataPaymentStatus(creditAccountPaymentRequest,payment,deletedAccountDetails, response);
        assertThat(mockLOG.getInfoLogs().isEmpty()).isTrue();
    }

    @Test
    public void testSetPaymentStatusWithValidationFailures() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        JSONObject responseBody = new JSONObject();
        responseBody.put("error_code", "3");
        responseBody.put("description", "Test message describing validation failures.");
        ResponseEntity<JSONObject> response = new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
        pbaStatusErrorMapper.setLiberataPaymentStatus(creditAccountPaymentRequest,payment,activeAccountDetails, response);
        assertThat(mockLOG.getInfoLogs().getFirst()).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ACTIVE PaymentStatus : failed - Test message describing validation failures.");
    }

    @Test
    public void testSetServiceRequestPaymentStatusWithValidationFailures() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        Payment serviceRequestPayment = Payment.paymentWith().ccdCaseNumber("service-request-case").build();
        JSONObject responseBody = new JSONObject();
        responseBody.put("error_code", "3");
        responseBody.put("description", "Service request validation failure.");
        ResponseEntity<JSONObject> response = new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);

        pbaStatusErrorMapper.setServiceRequestPaymentStatus(
            new BigDecimal("25.00"),
            serviceRequestPayment,
            activeAccountDetails,
            response
        );

        assertThat(serviceRequestPayment.getPaymentStatus().getName()).isEqualTo("failed");
        assertThat(serviceRequestPayment.getStatusHistories()).hasSize(1);
        assertThat(serviceRequestPayment.getStatusHistories().getFirst().getStatus()).isEqualTo("failed");
        assertThat(serviceRequestPayment.getStatusHistories().getFirst().getMessage()).isEqualTo("Service request validation failure.");
        assertThat(mockLOG.getInfoLogs().getFirst()).isEqualTo("CreditAccountPayment received for ccdCaseNumber : service-request-case Liberata AccountStatus : ACTIVE PaymentStatus : failed - Service request validation failure.");
    }

}
