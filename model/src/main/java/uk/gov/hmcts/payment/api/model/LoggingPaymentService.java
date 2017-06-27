package uk.gov.hmcts.payment.api.model;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoggingPaymentService implements PaymentService<Payment, Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingPaymentService.class);

    private static final String PAYMENT_ID = "paymentId";
    private static final String USER_ID = "userId";
    private static final String EVENT_TYPE = "eventType";
    private static final String AMOUNT = "amount";
    private static final String REFERENCE = "reference";

    private final UserIdSupplier userIdSupplier;
    private final PaymentService<Payment, Integer> delegate;

    @Autowired
    public LoggingPaymentService(UserIdSupplier userIdSupplier, PaymentService<Payment, Integer> delegate) {
        this.userIdSupplier = userIdSupplier;
        this.delegate = delegate;
    }

    @Override
    public Payment create(int amount, @NonNull String reference, @NonNull String description, @NonNull String returnUrl) {
        Payment payment = delegate.create(amount, reference, description, returnUrl);
        LOG.info("Payment event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, payment.getId(),
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "create",
            AMOUNT, payment.getAmount(),
            REFERENCE, payment.getReference()
        )));
        return payment;
    }

    @Override
    public Payment retrieve(@NonNull Integer id) {
        return delegate.retrieve(id);
    }

    @Override
    public void cancel(@NonNull Integer id) {
        delegate.cancel(id);
        LOG.info("Payment event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, id,
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "cancel"
        )));
    }

    @Override
    public void refund(@NonNull Integer id, int amount, int refundAmountAvailable) {
        delegate.refund(id, amount, refundAmountAvailable);
        LOG.info("Payment event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, id,
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "refund",
            AMOUNT, amount
        )));
    }
}
