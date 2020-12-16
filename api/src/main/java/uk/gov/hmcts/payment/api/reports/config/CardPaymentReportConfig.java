package uk.gov.hmcts.payment.api.reports.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.ReconciliationPaymentDto;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CardPaymentReportConfig implements PaymentReportConfig {

    private static final String CARD_PAYMENTS_HEADER = "Service,Payment Group reference,Payment reference," +
        "CCD reference,Case reference,Payment created date,Payment status updated date,Payment status," +
        "Payment channel,Payment method,Payment amount,Site id,Fee code,Version,Calculated amount,Memo line,NAC," +
        "Fee volume";

    private static final String CARD_PAYMENTS_CSV_FILE_PREFIX = "hmcts_card_payments_";

    @Value("${card.payments.email.from:dummy}")
    private String from;
    @Value("${card.payments.email.to:dummy}")
    private String[] to;
    @Value("${card.payments.email.subject:dummy}")
    private String subject;
    @Value("${card.payments.email.message:dummy}")
    private String message;
    @Value("${card.payments.report.scheduler.enabled:false}")
    private boolean enabled;

    @Override
    public PaymentReportType getType() {
        return PaymentReportType.CARD;
    }

    @Override
    public String getCsvHeader() {
        return CARD_PAYMENTS_HEADER;
    }

    @Override
    public String getCsvRecord(ReconciliationPaymentDto paymentDto) {
        return paymentDto.toCardPaymentCsv();
    }

    @Override
    public String getCsvFileNamePrefix() {
        return CARD_PAYMENTS_CSV_FILE_PREFIX;
    }
}
