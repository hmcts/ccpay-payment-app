package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.ImmutableMap;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.List;

@Component
public class LoggingPaymentRecordService implements PaymentRecordService<PaymentFeeLink, String> {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingPaymentRecordService.class);

    private static final String PAYMENT_ID = "paymentId";
    private static final String USER_ID = "userId";
    private static final String EVENT_TYPE = "eventType";
    private static final String AMOUNT = "amount";
    private static final String REFERENCE = "reference";
    private static final String CCD_CASE_NUMBER = "ccdCaseNumber";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String CURRENCY = "currency";
    private static final String FEES = "fees";

    private final UserIdSupplier userIdSupplier;
    private final PaymentRecordService<PaymentFeeLink, String> delegate;

    @Autowired
    public LoggingPaymentRecordService(UserIdSupplier userIdSupplier, PaymentRecordService<PaymentFeeLink, String> delegate) {
        this.userIdSupplier = userIdSupplier;
        this.delegate = delegate;
    }


    @Override
    public PaymentFeeLink recordPayment(Payment recordPayment, List<PaymentFee> fees, String paymentGroupReference) throws CheckDigitException {
        PaymentFeeLink paymentFeeLink = delegate.recordPayment(recordPayment, fees, paymentGroupReference);

        Payment payment = paymentFeeLink.getPayments().get(0);
        LOG.info("Payment event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, payment.getId(),
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "recordPayment",
            AMOUNT, payment.getAmount(),
            REFERENCE, payment.getReference()
        )));
        return paymentFeeLink;
    }
}
