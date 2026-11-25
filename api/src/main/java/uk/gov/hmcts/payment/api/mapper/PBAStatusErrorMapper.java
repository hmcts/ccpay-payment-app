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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Component
public class PBAStatusErrorMapper {

    private static final String FAILED = "failed";
    private static final Logger LOG = LoggerFactory.getLogger(PBAStatusErrorMapper.class);

    private static final String EXCEEDED_CREDIT_LIMIT_MESSAGE = "Payment request failed. PBA account {tradingName} have insufficient funds available";
    private static final String EXCEEDED_CREDIT_LIMIT = "Exceeded credit limit.";
    public static  String ON_HOLD="CUSTOMER ON HOLD";
    public static  String DELETED="DELETED";

    public static final Map<String, String> PBA_ERROR_CODE_MAP = Map.of(
        "Exceeded credit limit.", "CA-E0001",
        "Account not found.", "CA-E0004",
        "Validation failed.", "CA-E0005",
        "Duplicate payment detected.", "CA-E0006",
        "Unauthenticated.", "CA-E0007"
    );

    public static final Map<String, String> PBA_REASON_CODE_MAP = Map.of(
        DELETED, "CA-E0004",
        ON_HOLD, "CA-E0003"
    );

    public static final Map<String, String> PBA_MESSAGES_CODE_MAP = Map.of(
        DELETED, "Your account is deleted",
        ON_HOLD, "Your account is on hold",
        EXCEEDED_CREDIT_LIMIT,EXCEEDED_CREDIT_LIMIT_MESSAGE
    );

    public void setPaymentStatusAndHistories(String accountNumber, Payment payment, PaymentAccountResponse  paymentAccountResponse) {

        LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {}  Account number  : {} ",
            payment.getCcdCaseNumber(), paymentAccountResponse.getStatus(), accountNumber);
        // Ensure that the status histories list is initialized if it's null or empty, due to the fact that the method is generic.
        if (payment.getStatusHistories() == null || payment.getStatusHistories().isEmpty()) {
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus() != null ? payment.getPaymentStatus().getName() : PaymentAccountResponseStatus.PENDING.getValue())
                .build()));
        }

        if (paymentAccountResponse.getStatus().equals(PaymentAccountResponseStatus.SUCCESS.getValue())) {

            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
            payment.getStatusHistories().getFirst().setStatus(PaymentAccountResponseStatus.SUCCESS.getValue());
            LOG.info("Payment request  was SUCCESS. " + paymentAccountResponse.getMessage() + " PBA account {}" + accountNumber);
        } else if (paymentAccountResponse.getStatus().equals(PaymentAccountResponseStatus.ERROR.getValue())) {

            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            LOG.info("Payment request failed. " + paymentAccountResponse.getMessage() + " PBA account {}" + accountNumber);

            // check if the reason code is present in the response and if it is mapped to an error code, this done for deleted and on hold accounts.
            if(paymentAccountResponse.getReason() != null ) {
                payment.getStatusHistories().getFirst().setStatus(payment.getPaymentStatus().getName());
                if (isAnAccuntOnHold(paymentAccountResponse)) {
                    payment.getStatusHistories().getFirst().setErrorCode(PBA_REASON_CODE_MAP.get(ON_HOLD));
                    payment.getStatusHistories().getFirst().setMessage(PBA_MESSAGES_CODE_MAP.get(ON_HOLD));
                    LOG.info("Payment request failed. PBA account {} is on hold and temporarily unavailable.", accountNumber);
                    return;
                } else if (isAnAccuntDeleted(paymentAccountResponse)) {
                    payment.getStatusHistories().getFirst().setErrorCode(PBA_REASON_CODE_MAP.get(DELETED));
                    payment.getStatusHistories().getFirst().setMessage(PBA_MESSAGES_CODE_MAP.get(DELETED));
                    LOG.info("Payment request failed. PBA account {} has been deleted and is no longer available.", accountNumber);
                    return;
                }
            }
            payment.getStatusHistories().getFirst().setStatus(payment.getPaymentStatus().getName());
            payment.getStatusHistories().getFirst().setErrorCode(PBA_ERROR_CODE_MAP.get(paymentAccountResponse.getMessage()));
            payment.getStatusHistories().getFirst().setMessage(getMessageForErrorCode(paymentAccountResponse));
        }
    }

    private boolean isAnAccuntOnHold(PaymentAccountResponse paymentAccountResponse) {
        return paymentAccountResponse.getReason() != null && (paymentAccountResponse.getReason().contains(ON_HOLD));
    }

    private boolean isAnAccuntDeleted(PaymentAccountResponse paymentAccountResponse) {
        return paymentAccountResponse.getReason() != null && (paymentAccountResponse.getReason().contains(DELETED));
    }

    // This method returns a custom message for the "Exceeded credit limit." error code, including the trading name of the account.
    private String getMessageForErrorCode(PaymentAccountResponse paymentAccountResponse) {

        if (paymentAccountResponse.getMessage().equals("Exceeded credit limit.")){
            return PBA_MESSAGES_CODE_MAP.get(paymentAccountResponse.getMessage()).replace("{tradingName}", paymentAccountResponse.getTrading_name());
        }
        return paymentAccountResponse.getMessage();
    }

}
