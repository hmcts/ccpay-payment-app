package uk.gov.hmcts.payment.api.mapper;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponse;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponseStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;

import static nl.altindag.log.LogCaptor.forClass;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.api.util.AccountStatus.*;
import static org.mockito.Mockito.*;

// new imports for LiberataRealTimeAPI tests
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentRequest;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.service.LiberataRealTimeAPI;

@RunWith(MockitoJUnitRunner.class)
public class PBAStatusErrorMapperTest {
    private static CreditAccountPaymentRequest creditAccountPaymentRequest;
    private static Payment payment;

    private static AccountDto activeAccountDetails;
    private static AccountDto activeAccountInsufficientDetails;
    private static AccountDto onHoldAccountDetails;
    private static AccountDto inactiveAccountDetails;
    private static AccountDto deletedAccountDetails;

    private static LogCaptor mockLOG;

    @Mock
    private RestTemplate liberataRestTemplate;

    @Mock
    private uk.gov.hmcts.payment.api.mapper.liberata.identity.AccessTokenDtoToTokenResponseMapper accessTokenDtoToTokenResponseMapper;

    @InjectMocks
    private LiberataRealTimeAPI liberataRealTimeAPI;

    @BeforeClass
    public static void initiate(){
        mockLOG = forClass(PBAStatusErrorMapper.class);
        creditAccountPaymentRequest = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
                                        .amount(new BigDecimal("10.00"))
                                        .accountNumber("PBA1234567")
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
        inactiveAccountDetails = AccountDto.accountDtoWith()
            .accountName("account-name")
            .availableBalance(new BigDecimal("100.00"))
            .status(AccountStatus.INACTIVE).build();
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

    @AfterEach
    public void afterTest() {
        mockLOG.clearLogs();
    }

    @Test
    public void testSetPaymentStatusWithOnHold() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        pbaStatusErrorMapper.setPaymentStatus(creditAccountPaymentRequest, payment, onHoldAccountDetails);
        assertThat(mockLOG.getInfoLogs().get(1)).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ON_HOLD PaymentStatus : failed - Account is on hold!");
    }

    @Test
    public void testSetPaymentStatusWithInactive() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        Payment inactivePayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();

        pbaStatusErrorMapper.setPaymentStatus(creditAccountPaymentRequest, inactivePayment, inactiveAccountDetails);

        assertThat(mockLOG.getInfoLogs().get(1)).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : INACTIVE PaymentStatus : failed - Account is inactive!");
        assertThat(inactivePayment.getPaymentStatus().getName()).isEqualTo("failed");
        assertThat(inactivePayment.getStatusHistories()).hasSize(1);
        assertThat(inactivePayment.getStatusHistories().get(0).getErrorCode()).isEqualTo("CA-E0002");
        assertThat(inactivePayment.getStatusHistories().get(0).getMessage()).isEqualTo("Your account is inactive");
    }

    @Test
    public void testSetPaymentStatusWithActiveAndSufficientBalance() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        pbaStatusErrorMapper.setPaymentStatus(creditAccountPaymentRequest,payment,activeAccountDetails);
        assertThat(mockLOG.getInfoLogs().get(1)).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : ACTIVE PaymentStatus : success - Account Balance Sufficient!!!");
    }

    @Test
    public void testSetPaymentStatusWithDeletedStatus() {
        PBAStatusErrorMapper pbaStatusErrorMapper = new PBAStatusErrorMapper();
        pbaStatusErrorMapper.setPaymentStatus(creditAccountPaymentRequest,payment,deletedAccountDetails);
        assertThat(mockLOG.getInfoLogs().get(1)).isEqualTo("CreditAccountPayment received for ccdCaseNumber : ccd-number Liberata AccountStatus : DELETED PaymentStatus : failed - Account is deleted!");
    }

    @Test
    public void testSetPaymentStatusAndHistoriesSuccess() {
        PBAStatusErrorMapper mapper = new PBAStatusErrorMapper();
        PaymentAccountResponse resp = PaymentAccountResponse.paymentDtoWith()
            .status("success")
            .message(null)
            .build();

        Payment localPayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();

        // create an initial StatusHistory entry and spy the payment so mapper can call getStatusHistories().getFirst()
        LinkedList<StatusHistory> histories = new LinkedList<>(Collections.singletonList(StatusHistory.statusHistoryWith().build()));
        Payment spyPayment = spy(localPayment);
        when(spyPayment.getStatusHistories()).thenReturn(histories);

        mapper.setPaymentStatusAndHistories(creditAccountPaymentRequest, spyPayment, resp);

        assertThat(spyPayment.getPaymentStatus()).isNotNull();
        assertThat(spyPayment.getPaymentStatus().getName()).isEqualTo("success");
        // ensure the existing status history was updated to success
        assertThat(spyPayment.getStatusHistories()).isNotNull();
        assertThat(spyPayment.getStatusHistories()).hasSize(1);
        assertThat(spyPayment.getStatusHistories().get(0).getStatus()).isEqualTo(PaymentAccountResponseStatus.SUCCESS.getValue());
    }

    // --- setPaymentStatusAndHistories: ERROR cases without reason ---

    @ParameterizedTest
    @CsvSource({
        "'Exceeded credit limit.', CA-E0001, 'Payment request failed. PBA account PBA1234567 have insufficient funds available'",
        "'Account not found.', CA-E0004, 'Account not found.'",
        "'Validation failed.', CA-E0005, 'Validation failed.'",
        "'Duplicate payment detected.', CA-E0006, 'Duplicate payment detected.'",
        "'Unauthenticated.', CA-E0007, 'Unauthenticated.'"
    })
    public void testSetPaymentStatusAndHistoriesErrorsWithoutReason(String errorMessage, String expectedErrorCode,
                                                                    String expectedHistoryMessage) {
        Payment localPayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();
        localPayment.setStatusHistories(new LinkedList<>(Collections.singletonList(StatusHistory.statusHistoryWith().build())));
        PaymentAccountResponse response = PaymentAccountResponse.paymentDtoWith()
            .status("error")
            .message(errorMessage)
            .build();
        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("10.00"))
            .accountNumber("PBA1234567")
            .build();

        new PBAStatusErrorMapper().setPaymentStatusAndHistories(request, localPayment, response);

        assertThat(localPayment.getPaymentStatus().getName()).isEqualTo("failed");
        StatusHistory history = localPayment.getStatusHistories().get(0);
        assertThat(history.getStatus()).isEqualTo("failed");
        assertThat(history.getErrorCode()).isEqualTo(expectedErrorCode);
        assertThat(history.getMessage()).isEqualTo(expectedHistoryMessage);
    }

    // --- setPaymentStatusAndHistories: ERROR with reason = DELETED ---

    @Test
    public void testSetPaymentStatusAndHistoriesErrorReasonDeleted() {
        PBAStatusErrorMapper mapper = new PBAStatusErrorMapper();
        Payment localPayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();
        localPayment.setStatusHistories(new LinkedList<>(Collections.singletonList(StatusHistory.statusHistoryWith().build())));

        PaymentAccountResponse resp = PaymentAccountResponse.paymentDtoWith()
            .status("error")
            .message("Account not found.")
            .reason("DELETED")
            .build();

        mapper.setPaymentStatusAndHistories(creditAccountPaymentRequest, localPayment, resp);

        assertThat(localPayment.getPaymentStatus().getName()).isEqualTo("failed");
        StatusHistory history = localPayment.getStatusHistories().get(0);
        assertThat(history.getStatus()).isEqualTo("failed");
        assertThat(history.getErrorCode()).isEqualTo("CA-E0004");
        assertThat(history.getMessage()).isEqualTo("Your account is deleted");
    }

    // --- setPaymentStatusAndHistories: ERROR with reason = ON_HOLD ---

    @Test
    public void testSetPaymentStatusAndHistoriesErrorReasonOnHold() {
        PBAStatusErrorMapper mapper = new PBAStatusErrorMapper();
        Payment localPayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();
        localPayment.setStatusHistories(new LinkedList<>(Collections.singletonList(StatusHistory.statusHistoryWith().build())));

        PaymentAccountResponse resp = PaymentAccountResponse.paymentDtoWith()
            .status("error")
            .message("Account not active.")
            .reason("CUSTOMER ON HOLD - EXCEEDED CREDIT LIMIT")
            .build();

        mapper.setPaymentStatusAndHistories(creditAccountPaymentRequest, localPayment, resp);

        assertThat(localPayment.getPaymentStatus().getName()).isEqualTo("failed");
        StatusHistory history = localPayment.getStatusHistories().get(0);
        assertThat(history.getStatus()).isEqualTo("failed");
        assertThat(history.getErrorCode()).isEqualTo("CA-E0003");
        assertThat(history.getMessage()).isEqualTo("Your account is on hold");
    }

    // kept for backward compatibility — now replaced by the granular tests above
    @Test
    public void testSetPaymentStatusAndHistoriesErrors() {
        // this test is superseded by the individual tests above; kept as a smoke test
        PBAStatusErrorMapper mapper = new PBAStatusErrorMapper();

        for (Map.Entry<String, String> entry : PBAStatusErrorMapper.PBA_ERROR_CODE_MAP.entrySet()) {
            String message = entry.getKey();
            String expectedErrorCode = entry.getValue();

            Payment localPayment = Payment.paymentWith().ccdCaseNumber("ccd-number").build();
            localPayment.setStatusHistories(new LinkedList<>(Collections.singletonList(StatusHistory.statusHistoryWith().build())));

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

            // for "Exceeded credit limit." the message contains the account number
            if (message.equals("Exceeded credit limit.")) {
                assertThat(history.getMessage()).contains("PBA1234567");
            } else {
                assertThat(history.getMessage()).isEqualTo(message);
            }
        }
    }



    // -------------------- New tests for LiberataRealTimeAPI.payByAccount --------------------

    @Test
    public void testPayByAccountReturnsSuccessResponse() {
        // prepare request
        PaymentByAccountRequest pbaReq = PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber("PBA123")
            .payment(PaymentRequest.paymentDtoWith().amount("10.00").serviceName("TEST").build())
            .build();

        PaymentAccountRequest request = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(pbaReq)
            .build();

        // prepare token and stub getValidToken by spying the injected instance
        TokenResponse token = new TokenResponse("plain-text-token", 1000000L, System.currentTimeMillis());
        LiberataRealTimeAPI spyApi = spy(liberataRealTimeAPI);
        doReturn(token).when(spyApi).getValidToken();

        // prepare expected PaymentAccountResponse
        PaymentAccountResponse expected = PaymentAccountResponse.paymentDtoWith()
            .status("success")
            .message(null)
            .build();

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PaymentAccountResponse.class)))
            .thenReturn(ResponseEntity.ok(expected));

        PaymentAccountResponse resp = spyApi.payByAccount(request);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo("success");

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(PaymentAccountResponse.class));
    }

    @Test
    public void testPayByAccountHandlesHttpClientErrorExceptionWithBody() {
        PaymentByAccountRequest pbaReq = PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber("PBA123")
            .payment(PaymentRequest.paymentDtoWith().amount("10.00").serviceName("TEST").build())
            .build();

        PaymentAccountRequest request = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(pbaReq)
            .build();

        TokenResponse token = new TokenResponse("plain-text-token", 1000000L, System.currentTimeMillis());
        LiberataRealTimeAPI spyApi = spy(liberataRealTimeAPI);
        doReturn(token).when(spyApi).getValidToken();

        PaymentAccountResponse errorResp = PaymentAccountResponse.paymentDtoWith()
            .status("error")
            .message("insufficient funds")
            .build();

        HttpClientErrorException httpEx = mock(HttpClientErrorException.class);
        when(httpEx.getResponseBodyAs(PaymentAccountResponse.class)).thenReturn(errorResp);

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PaymentAccountResponse.class)))
            .thenThrow(httpEx);

        PaymentAccountResponse resp = spyApi.payByAccount(request);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo("error");
        assertThat(resp.getMessage()).isEqualTo("insufficient funds");

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(PaymentAccountResponse.class));
    }

    @Test
    public void testPayByAccountThrowsWhenErrorBodyParsingFails() {
        PaymentByAccountRequest pbaReq = PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber("PBA123")
            .payment(PaymentRequest.paymentDtoWith().amount("10.00").serviceName("TEST").build())
            .build();

        PaymentAccountRequest request = PaymentAccountRequest.paymentAccountRequestWith()
            .paymentByAccountRequest(pbaReq)
            .build();

        TokenResponse token = new TokenResponse("plain-text-token", 1000000L, System.currentTimeMillis());
        LiberataRealTimeAPI spyApi = spy(liberataRealTimeAPI);
        doReturn(token).when(spyApi).getValidToken();

        HttpClientErrorException httpEx = mock(HttpClientErrorException.class);
        when(httpEx.getResponseBodyAs(PaymentAccountResponse.class)).thenThrow(new RuntimeException("parse fail"));

        when(liberataRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(PaymentAccountResponse.class)))
            .thenThrow(httpEx);

        try {
            spyApi.payByAccount(request);
            org.junit.Assert.fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).contains("Error posting payment to Liberata");
            assertThat(ex.getCause()).isNotNull();
            assertThat(ex.getCause().getMessage()).isEqualTo("parse fail");
        }

        verify(liberataRestTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(PaymentAccountResponse.class));
    }

}
