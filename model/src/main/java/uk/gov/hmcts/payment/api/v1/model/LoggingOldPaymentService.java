package uk.gov.hmcts.payment.api.v1.model;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoggingOldPaymentService implements PaymentService<PaymentOld, Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingOldPaymentService.class);

    private static final String PAYMENT_ID = "paymentId";
    private static final String USER_ID = "userId";
    private static final String EVENT_TYPE = "eventType";
    private static final String AMOUNT = "amount";
    private static final String REFERENCE = "reference";

    private final UserIdSupplier userIdSupplier;
    private final PaymentService<PaymentOld, Integer> delegate;

    @Autowired
    public LoggingOldPaymentService(UserIdSupplier userIdSupplier, PaymentService<PaymentOld, Integer> delegate) {
        this.userIdSupplier = userIdSupplier;
        this.delegate = delegate;
    }

    @Override
    public PaymentOld create(int amount, @NonNull String reference, @NonNull String description, @NonNull String returnUrl, String language) {
        PaymentOld paymentOld = delegate.create(amount, reference, description, returnUrl,null);
        LOG.info("PaymentOld event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, paymentOld.getId(),
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "create",
            AMOUNT, paymentOld.getAmount(),
            REFERENCE, paymentOld.getReference()
        )));
        return paymentOld;
    }

    @Override
    public PaymentOld retrieve(@NonNull Integer id) {
        return delegate.retrieve(id);
    }

    @Override
    public void cancel(@NonNull Integer id) {
        delegate.cancel(id);
        LOG.info("PaymentOld event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, id,
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "cancel"
        )));
    }

    @Override
    public void refund(@NonNull Integer id, int amount, int refundAmountAvailable) {
        delegate.refund(id, amount, refundAmountAvailable);
        LOG.info("PaymentOld event", StructuredArguments.entries(ImmutableMap.of(
            PAYMENT_ID, id,
            USER_ID, userIdSupplier.get(),
            EVENT_TYPE, "refund",
            AMOUNT, amount
        )));
    }
}
