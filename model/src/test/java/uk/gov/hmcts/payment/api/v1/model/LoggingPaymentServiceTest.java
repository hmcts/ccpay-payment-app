package uk.gov.hmcts.payment.api.v1.model;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.payment.api.v1.model.logging.TestAppender;

import static ch.qos.logback.classic.Level.INFO;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

public class LoggingPaymentServiceTest {

    private final TestAppender testAppender = new TestAppender();
    private final LoggingPaymentService loggingPaymentService = new LoggingPaymentService(() -> "userId", new FakePaymentService());

    @Before
    public void addAppender() throws Exception {
        ((Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME)).addAppender(testAppender);
    }

    @After
    public void removeAppender() throws Exception {
        ((Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME)).detachAppender(testAppender);
    }

    @Test
    public void createPaymentShouldBeLogged() {
        loggingPaymentService.create(100, "reference", "any", "any");
        testAppender.assertEvent(0, INFO, "Payment event", ImmutableMap.of(
            "paymentId", 1,
            "userId", "userId",
            "eventType", "create",
            "amount", 100,
            "reference", "reference"
        ));
    }

    @Test
    public void cancelPaymentShouldBeLogged() {
        loggingPaymentService.cancel(2);
        testAppender.assertEvent(0, INFO, "Payment event", ImmutableMap.of(
            "paymentId", 2,
            "userId", "userId",
            "eventType", "cancel"
        ));
    }

    @Test
    public void refundPaymentShouldBeLogged() {
        loggingPaymentService.refund(3, 10, 500);
        testAppender.assertEvent(0, INFO, "Payment event", ImmutableMap.of(
            "paymentId", 3,
            "userId", "userId",
            "eventType", "refund",
            "amount", 10
        ));
    }

    private static class FakePaymentService implements PaymentService<Payment, Integer> {
        @Override
        public Payment create(int amount, String reference, String description, String returnUrl) {
            return Payment.paymentWith().id(1).amount(amount).reference(reference).build();
        }

        @Override
        public Payment retrieve(Integer integer) {
            return null;
        }

        @Override
        public void cancel(Integer integer) {
        }

        @Override
        public void refund(Integer integer, int amount, int refundAmountAvailabie) {
        }
    }
}
