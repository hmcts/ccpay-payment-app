package uk.gov.hmcts.payment.api.mapper;

import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

@Component
public class PBAStatusErrorMapper {

    private static final String FAILED = "failed";
    private static final Logger LOG = LoggerFactory.getLogger(PBAStatusErrorMapper.class);
    private static final String EXCEEDED_CREDIT_LIMIT = "1";
    private static final String ACCOUNT_NOT_FOUND = "2";
    private static final String VALIDATION_ERRORS = "3";
    private static final String ACCOUNT_NOT_ACTIVE = "4";



    public void setLiberataPaymentStatus(CreditAccountPaymentRequest creditAccountPaymentRequest,
                                         Payment payment, AccountDto accountDetails,
                                         ResponseEntity<JSONObject> response) {
        HttpStatusCode statusCode = response.getStatusCode();
        String liberataErrorCode = extractValue("error_code", response);
        String description = extractValue("description", response);


        if (statusCode == HttpStatus.OK) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance Sufficient!!!",
                payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (statusCode == HttpStatus.FORBIDDEN) {
            switch (liberataErrorCode) {
                case EXCEEDED_CREDIT_LIMIT -> {
                    recordForbiddenFailure(payment, accountDetails, "CA-E0001", description);
                LOG.info("Payment request failed. PBA account {} has insufficient funds available. Requested payment was {} ",
                    accountDetails.getAccountName(), creditAccountPaymentRequest.getAmount());
                }
                case ACCOUNT_NOT_FOUND ->
                    recordForbiddenFailure(payment, accountDetails, "CA-E0004", description);
                case ACCOUNT_NOT_ACTIVE ->
                    recordForbiddenFailure(payment, accountDetails, "CA-E0003", description);
            }
        } else if (statusCode == HttpStatus.BAD_REQUEST) {
            recordValidationFailure(payment, accountDetails, description);
        }
    }

    private String extractValue(String key, ResponseEntity<JSONObject> response) {
        return Optional.ofNullable(response.getBody())
            .map(body -> body.get(key))
            .map(Object::toString)
            .orElse("");
    }

    private void recordForbiddenFailure(Payment payment, AccountDto accountDetails, String errorCode, String description) {
        payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
        payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
            .status(payment.getPaymentStatus().getName())
            .errorCode(errorCode)
            .message(description)
            .build()));
        LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - {}",
            payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName(), description);
    }

    private void recordValidationFailure(Payment payment, AccountDto accountDetails, String description) {
        payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
        payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
            .status(payment.getPaymentStatus().getName())
            .message(description)
            .build()));
        LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - {}",
            payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName(), description);
    }


    public void setPaymentStatus(CreditAccountPaymentRequest creditAccountPaymentRequest, Payment payment, AccountDto accountDetails) {
        if (accountDetails.getStatus() == AccountStatus.ACTIVE && isAccountBalanceSufficient(accountDetails.getAvailableBalance(),
            creditAccountPaymentRequest.getAmount())) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance Sufficient!!!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.ACTIVE) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0001")
                .message("You have insufficient funds available")
                .message("Payment request failed. PBA account " + accountDetails.getAccountName()
                    + " have insufficient funds available").build()));
            LOG.info("Payment request failed. PBA account {} has insufficient funds available." +
                    " Requested payment was {} where available balance is {}",
                accountDetails.getAccountName(), creditAccountPaymentRequest.getAmount(),
                accountDetails.getAvailableBalance());
            LOG.info("ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance InSufficient!!!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.ON_HOLD) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0003")
                .message("Your account is on hold")
                .build()));
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account is on hold!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.DELETED) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0004")
                .message("Your account is deleted")
                .build()));
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account is deleted!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        }
    }

    public void setServiceRequestPaymentStatus(BigDecimal amount, Payment payment, AccountDto accountDetails) {
        setPaymentStatus(CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith().amount(amount).build(), payment, accountDetails);
    }


    private boolean isAccountBalanceSufficient(BigDecimal availableBalance, BigDecimal paymentAmount) {
        return availableBalance.compareTo(paymentAmount) >= 0;
    }
}
