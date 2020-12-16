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
public class BarPaymentReportConfig implements PaymentReportConfig {

    private static final String PAYMENTS_HEADER = "Service,Payment Group reference,Payment reference," +
        "CCD reference,Case reference,Organisation name,Customer internal reference,PBA Number,Payment created date," +
        "Payment status updated date,Payment status,Payment channel,Payment method,Payment amount,Site id,Fee code," +
        "Version,Calculated amount,Memo line,NAC,Fee volume";

    private static final String PAYMENTS_CSV_FILE_PREFIX = "hmcts_bar_payments_";

    @Value("${bar.payments.email.from:dummy}")
    private String from;
    @Value("${bar.payments.email.to:dummy}")
    private String[] to;
    @Value("${bar.payments.email.subject:dummy}")
    private String subject;
    @Value("${bar.payments.email.message:dummy}")
    private String message;
    @Value("${bar.payments.report.scheduler.enabled:false}")
    private boolean enabled;

    @Override
    public PaymentReportType getType() {
        return PaymentReportType.DIGITAL_BAR;
    }

    @Override
    public String getCsvHeader() {
        return PAYMENTS_HEADER;
    }

    @Override
    public String getCsvRecord(ReconciliationPaymentDto paymentDto) {
        return paymentDto.toPaymentCsv();
    }

    @Override
    public String getCsvFileNamePrefix() {
        return PAYMENTS_CSV_FILE_PREFIX;
    }
}
