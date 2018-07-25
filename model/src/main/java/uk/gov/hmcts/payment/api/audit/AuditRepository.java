package uk.gov.hmcts.payment.api.audit;

import org.springframework.scheduling.annotation.Async;
import uk.gov.hmcts.payment.api.model.Payment;

import java.util.Map;

@Async
public interface AuditRepository {

    void trackPaymentEvent(String name, Payment payment);

    void trackEvent(String name, Map<String, String> properties);
}
