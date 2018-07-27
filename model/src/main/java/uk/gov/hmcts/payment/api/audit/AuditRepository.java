package uk.gov.hmcts.payment.api.audit;

import org.springframework.scheduling.annotation.Async;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.util.List;
import java.util.Map;

@Async
public interface AuditRepository {

    void trackPaymentEvent(String name, Payment payment, List<PaymentFee> fees);

    void trackEvent(String name, Map<String, String> properties);
}
