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


        switch (statusCode) {
            case HttpStatus.OK -> {
                payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
                LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance Sufficient!!!",
                    payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
            }
            case HttpStatus.FORBIDDEN -> {
                switch (liberataErrorCode) {
                    case EXCEEDED_CREDIT_LIMIT -> {
                        recordForbiddenFailure(payment, accountDetails, "CA-E0001", description);
                        LOG.info("Payment request failed. PBA account {} has insufficient funds available. Requested payment was {} ",
                            accountDetails.getAccountName(), creditAccountPaymentRequest.getAmount());
                    }
                    case ACCOUNT_NOT_FOUND -> recordForbiddenFailure(payment, accountDetails, "CA-E0004", description);
                    case ACCOUNT_NOT_ACTIVE -> recordForbiddenFailure(payment, accountDetails, "CA-E0003", description);
                }
            }
            case HttpStatus.BAD_REQUEST -> recordValidationFailure(payment, accountDetails, description);
            default -> throw new IllegalStateException("Unexpected status code value: " + statusCode);
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


    public void setServiceRequestPaymentStatus(BigDecimal amount, Payment payment, AccountDto accountDetails, ResponseEntity<JSONObject> liberataResponse) {
        setLiberataPaymentStatus(CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith().amount(amount).build(), payment, accountDetails, liberataResponse);
    }

}
