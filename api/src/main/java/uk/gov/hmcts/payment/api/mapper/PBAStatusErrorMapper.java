package uk.gov.hmcts.payment.api.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponse;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponseStatus;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.util.LogSanitizer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Component
public class PBAStatusErrorMapper {

    private static final String FAILED = "failed";
    private static final Logger LOG = LoggerFactory.getLogger(PBAStatusErrorMapper.class);

    private static final String EXCEEDED_CREDIT_LIMIT_MESSAGE = "Payment request failed. PBA account {accountNumber} have insufficient funds available";
    private static final String EXCEEDED_CREDIT_LIMIT = "Exceeded credit limit.";
    private static final String EXCEEDED_CODE = "CA-E0001";
    private static final String NOT_FOUND_CODE = "CA-E0004";
    private static final String VALIDATION_CODE = "CA-E0005";
    private static final String DUPLICATE_CODE = "CA-E0006";
    private static final String UNAUTHENTICATED_CODE = "CA-E0007";
    public static  String ON_HOLD="CUSTOMER ON HOLD - EXCEEDED CREDIT LIMIT";

    public static  String DELETED="DELETED";

    public static final Map<String, String> PBA_ERROR_CODE_MAP = Map.of(
        EXCEEDED_CREDIT_LIMIT, EXCEEDED_CODE,
        "Account not found.", NOT_FOUND_CODE,
        "Validation failed.", VALIDATION_CODE,
        "Duplicate payment detected.", DUPLICATE_CODE,
        "Unauthenticated.", UNAUTHENTICATED_CODE
    );

    public static final Map<String, String> PBA_REASON_CODE_MAP = Map.of(
        DELETED, NOT_FOUND_CODE,
        ON_HOLD, "CA-E0003"
    );

    public static final Map<String, String> PBA_MESSAGES_CODE_MAP = Map.of(
        DELETED, "Your account is deleted",
        ON_HOLD, "Your account is on hold",
        EXCEEDED_CREDIT_LIMIT, EXCEEDED_CREDIT_LIMIT_MESSAGE
    );

    public void setPaymentStatus(CreditAccountPaymentRequest creditAccountPaymentRequest, Payment payment, AccountDto accountDetails) {
        LOG.info("CreditAccountPayment received for ccdCaseNumber: {}: Payload {}",
            LogSanitizer.sanitize(payment.getCcdCaseNumber()), LogSanitizer.sanitize(String.valueOf(accountDetails)));
        if (accountDetails.getStatus() == AccountStatus.ACTIVE && isAccountBalanceSufficient(accountDetails.getAvailableBalance(),
            creditAccountPaymentRequest.getAmount())) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance Sufficient!!!",
                LogSanitizer.sanitize(payment.getCcdCaseNumber()), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.ACTIVE) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode(EXCEEDED_CODE)
                .message("You have insufficient funds available")
                .message("Payment request failed. PBA account " + accountDetails.getAccountName()
                    + " have insufficient funds available").build()));
            LOG.info("Payment request failed. PBA account {} has insufficient funds available." +
                    " Requested payment was {} where available balance is {}",
                LogSanitizer.sanitize(accountDetails.getAccountName()), creditAccountPaymentRequest.getAmount(),
                accountDetails.getAvailableBalance());
            LOG.info("ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance InSufficient!!!",
                LogSanitizer.sanitize(payment.getCcdCaseNumber()), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.INACTIVE) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0002")
                .message("Your account is inactive")
                .build()));
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account is inactive!",
                LogSanitizer.sanitize(payment.getCcdCaseNumber()), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.ON_HOLD) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0003")
                .message("Your account is on hold")
                .build()));
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account is on hold!",
                LogSanitizer.sanitize(payment.getCcdCaseNumber()), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.DELETED) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode(NOT_FOUND_CODE)
                .message("Your account is deleted")
                .build()));
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account is deleted!",
                LogSanitizer.sanitize(payment.getCcdCaseNumber()), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        }
    }

    public void setPaymentStatusAndHistories(CreditAccountPaymentRequest creditAccountPaymentRequest, Payment payment, PaymentAccountResponse  paymentAccountResponse) {

        LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {}  Account number  : {} ",
            LogSanitizer.sanitize(payment.getCcdCaseNumber()), paymentAccountResponse.getStatus(),
            LogSanitizer.sanitize(creditAccountPaymentRequest.getAccountNumber()));

        if (paymentAccountResponse.getStatus().equals(PaymentAccountResponseStatus.SUCCESS.getValue())) {

            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
            payment.getStatusHistories().getFirst().setStatus(PaymentAccountResponseStatus.SUCCESS.getValue());
            LOG.info("Payment request was SUCCESS. {} PBA account {}",
                LogSanitizer.sanitize(paymentAccountResponse.getMessage()),
                LogSanitizer.sanitize(creditAccountPaymentRequest.getAccountNumber()));
        } else if (paymentAccountResponse.getStatus().equals(PaymentAccountResponseStatus.ERROR.getValue())) {

            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            LOG.info("Payment request failed. {} PBA account {}",
                LogSanitizer.sanitize(paymentAccountResponse.getMessage()),
                LogSanitizer.sanitize(creditAccountPaymentRequest.getAccountNumber()));

            // check if the reason code is present in the response and if it is mapped to an error code, this done for deleted and on hold accounts.
            if(paymentAccountResponse.getReason() != null && PBA_REASON_CODE_MAP.containsKey(paymentAccountResponse.getReason())) {
                payment.getStatusHistories().getFirst().setStatus(payment.getPaymentStatus().getName());
                payment.getStatusHistories().getFirst().setErrorCode(PBA_REASON_CODE_MAP.get(paymentAccountResponse.getReason()));
                payment.getStatusHistories().getFirst().setMessage(PBA_MESSAGES_CODE_MAP.get(paymentAccountResponse.getReason()));
                return;
            }
            payment.getStatusHistories().getFirst().setStatus(payment.getPaymentStatus().getName());
            payment.getStatusHistories().getFirst().setErrorCode(PBA_ERROR_CODE_MAP.get(paymentAccountResponse.getMessage()));
            payment.getStatusHistories().getFirst().setMessage(getMessageForErrorCode(paymentAccountResponse, creditAccountPaymentRequest));
        }
    }
    // This method is used to get the message for the error code. If the error code is "Exceeded credit limit.",
    // it will return a message with the account number. Otherwise, it will return the message from the paymentAccountResponse.
    private String getMessageForErrorCode(PaymentAccountResponse paymentAccountResponse,CreditAccountPaymentRequest creditAccountPaymentRequest ) {

        if (paymentAccountResponse.getMessage().equals(EXCEEDED_CREDIT_LIMIT)){
            return PBA_MESSAGES_CODE_MAP.get(paymentAccountResponse.getMessage()).replace("{accountNumber}", creditAccountPaymentRequest.getAccountNumber());
        }
        return paymentAccountResponse.getMessage();
    }

    public void setServiceRequestPaymentStatus(BigDecimal amount, Payment payment, AccountDto accountDetails) {
        setPaymentStatus(CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith().amount(amount).build(), payment, accountDetails);
    }


    private boolean isAccountBalanceSufficient(BigDecimal availableBalance, BigDecimal paymentAmount) {
        return availableBalance.compareTo(paymentAmount) >= 0;
    }
}
