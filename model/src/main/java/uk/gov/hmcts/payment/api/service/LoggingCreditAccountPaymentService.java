package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.ImmutableMap;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.Date;
import java.util.List;

@Component
public class LoggingCreditAccountPaymentService implements CreditAccountPaymentService<PaymentFeeLink, String> {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingCreditAccountPaymentService.class);

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
    private final CreditAccountPaymentService<PaymentFeeLink, String> delegate;

    @Autowired
    public LoggingCreditAccountPaymentService(UserIdSupplier userIdSupplier, CreditAccountPaymentService<PaymentFeeLink, String> delegate) {
        this.userIdSupplier = userIdSupplier;
        this.delegate = delegate;

    }

    @Override
    public PaymentFeeLink create(Payment payment, List<PaymentFee> fees, String paymentGroupReference) throws CheckDigitException {
        PaymentFeeLink paymentFeeLink = delegate.create(payment, fees, paymentGroupReference);

        paymentFeeLink.getPayments().forEach(p -> {
            LOG.info("Payment event", StructuredArguments.entries(ImmutableMap.of(
                PAYMENT_ID, p.getId(),
                USER_ID, userIdSupplier.get(),
                EVENT_TYPE, "create",
                AMOUNT, p.getAmount(),
                REFERENCE, p.getReference()
            )));
        });

        return paymentFeeLink;
    }

    @Override
    public PaymentFeeLink retrieveByPaymentGroupReference(String paymentGroupReference) {
        LOG.info("Get payments by payment group reference: {}", paymentGroupReference);
        return delegate.retrieveByPaymentGroupReference(paymentGroupReference);
    }

    @Override
    public PaymentFeeLink retrieveByPaymentReference(String paymentReference) {
        LOG.info("Get payment by payment reference: {}", paymentReference);
        return delegate.retrieveByPaymentReference(paymentReference);
    }

    @Override
    public List<PaymentFeeLink> search(Date startDate, Date endDate) {
        LOG.info("Searching for payments between {} and {}", startDate, endDate);

        List<PaymentFeeLink> paymentFeeLinks =  delegate.search(startDate, endDate);
        LOG.info("PaymentFeeLinks found: {}", paymentFeeLinks.size());
        return paymentFeeLinks;
    }

    @Override
    public void deleteByPaymentReference(String paymentReference) {
    }
}
