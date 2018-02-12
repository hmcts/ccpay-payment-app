package uk.gov.hmcts.payment.api.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentReconciliationReportEmail extends Email {

    public CardPaymentReconciliationReportEmail(@Value("${card.payments.email.from}") String from,
                                                @Value("${card.payments.email.to}") String to,
                                                @Value("${card.payments.email.subject}") String subject,
                                                @Value("${card.payments.email.message}") String message) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.message = message;
    }
}
