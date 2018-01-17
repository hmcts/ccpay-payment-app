package uk.gov.hmcts.payment.api.model;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.List;

@Component
public class LoggingCardPaymentService implements CardPaymentService<PaymentFeeLink, Integer> {

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
    private final CardPaymentService<PaymentFeeLink, Integer> delegate;

    public LoggingCardPaymentService(UserIdSupplier userIdSupplier, CardPaymentService<PaymentFeeLink, Integer> delete) {
        this.userIdSupplier = userIdSupplier;
        this.delegate = delete;
    }

    @Override
    public PaymentFeeLink create(int amount, @NonNull String paymentReference, @NonNull String description, @NonNull String returnUrl,
                                 String ccdCaseNumber, String caseReference, String currency, String siteId, List<Fee> fees) {
        PaymentFeeLink paymentFeeLink = delegate.create(amount, paymentReference, description, returnUrl, ccdCaseNumber, caseReference, currency, siteId, fees);

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
    public PaymentFeeLink retrieve(Integer integer) {
        return null;
    }

    @Override
    public void cancel(Integer integer) {

    }

    @Override
    public void refund(Integer integer, int amount, int refundAmountAvailabie) {

    }

}
