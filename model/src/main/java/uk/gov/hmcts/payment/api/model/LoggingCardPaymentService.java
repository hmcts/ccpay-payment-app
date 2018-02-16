package uk.gov.hmcts.payment.api.model;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.Date;
import java.util.List;

@Component
public class LoggingCardPaymentService implements CardPaymentService<PaymentFeeLink, String> {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingCardPaymentService.class);

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
    private final CardPaymentService<PaymentFeeLink, String> delegate;

    public LoggingCardPaymentService(UserIdSupplier userIdSupplier, CardPaymentService<PaymentFeeLink, String> delete) {
        this.userIdSupplier = userIdSupplier;
        this.delegate = delete;
    }

    @Override
    public PaymentFeeLink create(int amount, @NonNull String paymentReference, @NonNull String description, @NonNull String returnUrl,
                                 String ccdCaseNumber, String caseReference, String currency, String siteId, String serviceType, List<Fee> fees) throws CheckDigitException {
        PaymentFeeLink paymentFeeLink = delegate.create(amount, paymentReference, description, returnUrl, ccdCaseNumber, caseReference, currency, siteId, serviceType, fees);

        Payment payment = paymentFeeLink.getPayments().get(0);
        LOG.info("Payment event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, payment.getId(),
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "create",
            AMOUNT, payment.getAmount(),
            REFERENCE, paymentFeeLink.getPaymentReference()
        )));
        return paymentFeeLink;
    }

    @Override
    public PaymentFeeLink retrieve(String paymentReference) {
        return delegate.retrieve(paymentReference);
    }

    @Override
    public void cancel(String paymentReference) {

    }

    @Override
    public void refund(String paymentReference, int amount, int refundAmountAvailabie) {

    }

    @Override
    public List<PaymentFeeLink> search(Date startDate, Date endDate) {
        LOG.info("Searching for payments between {} and {}", startDate, endDate);

        List<PaymentFeeLink> paymentFeeLinks =  delegate.search(startDate, endDate);
        LOG.info("PaymentFeeLinks found: {}", paymentFeeLinks.size());
        return paymentFeeLinks;
    }

}
