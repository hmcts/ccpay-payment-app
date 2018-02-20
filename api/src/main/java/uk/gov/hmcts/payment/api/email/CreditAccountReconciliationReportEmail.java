package uk.gov.hmcts.payment.api.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CreditAccountReconciliationReportEmail extends Email{

    public CreditAccountReconciliationReportEmail(@Value("${pba.payments.email.from}") String from,
                                                  @Value("${pba.payments.email.to}") String to,
                                                  @Value("${pba.payments.email.subject}") String subject,
                                                  @Value("${pba.payments.email.message}") String message) {
        this.from = from;
        this.to = to == null ? null:to.split(",");
        this.subject = subject;
        this.message = message;
    }
}

