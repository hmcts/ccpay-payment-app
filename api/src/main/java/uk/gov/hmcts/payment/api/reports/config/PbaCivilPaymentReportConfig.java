package uk.gov.hmcts.payment.api.reports.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PbaCivilPaymentReportConfig implements PaymentReportConfig {

    private static final String CREDIT_ACCOUNT_PAYMENTS_HEADER = "Service,Payment Group reference,Payment reference," +
        "CCD reference,Case reference,Organisation name,Customer internal reference,PBA Number,Payment created date," +
        "Payment status updated date,Payment status,Payment channel,Payment method,Payment amount,Site id,Fee code," +
        "Version,Calculated amount,Memo line,NAC,Fee volume";

    private static final String CREDIT_ACCOUNT_PAYMENTS_CSV_FILE_PREFIX = "hmcts_credit_account_payments_civil_";

    @Value("${pba.civil.payments.email.from:dummy}")
    private String from;
    @Value("${pba.civil.payments.email.to:dummy}")
    private String[] to;
    @Value("${pba.civil.payments.email.subject:dummy}")
    private String subject;
    @Value("${pba.civil.payments.email.message:dummy}")
    private String message;
    @Value("${pba.civil.payments.report.scheduler.enabled:false}")
    private boolean enabled;

    @Override
    public PaymentReportType getType() {
        return PaymentReportType.PBA_CIVIL;
    }

    @Override
    public String getCsvHeader() {
        return CREDIT_ACCOUNT_PAYMENTS_HEADER;
    }

    @Override
    public String getCsvRecord(PaymentDto paymentDto) {
        return paymentDto.toCreditAccountPaymentCsv();
    }

    @Override
    public String getCsvFileNamePrefix() {
        return CREDIT_ACCOUNT_PAYMENTS_CSV_FILE_PREFIX;
    }
}
