package uk.gov.hmcts.payment.api.reports.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.DuplicatePaymentDto;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DuplicatePaymentReportConfig implements PaymentReportConfig<DuplicatePaymentDto> {

    private static final String DUPLICATE_PAYMENTS_HEADER = "Date,Time,CCD Case Number,Service Type,Amount,"
        + "Payment Channel,Payment Method,Payment Link ID,Count";

    private static final String DUPLICATE_PAYMENTS_CSV_FILE_PREFIX = "hmcts_potential_duplicate_payments_";

    @Value("${duplicate.payments.email.from:dummy}")
    private String from;
    @Value("${duplicate.payments.email.to:dummy}")
    private String[] to;
    @Value("${duplicate.payments.email.subject:dummy}")
    private String subject;
    @Value("${duplicate.payments.email.message:dummy}")
    private String message;
    @Value("${duplicate.payments.report.scheduler.enabled:false}")
    private boolean enabled;

    @Override
    public PaymentReportType getType() { return PaymentReportType.DUPLICATE_PAYMENT; }

    @Override
    public String getCsvHeader() {
        return DUPLICATE_PAYMENTS_HEADER;
    }

    @Override
    public String getCsvRecord(DuplicatePaymentDto duplicatePaymentDto) {
        return duplicatePaymentDto.toDuplicatePaymentCsv();
    }

    @Override
    public String getCsvFileNamePrefix() {
        return DUPLICATE_PAYMENTS_CSV_FILE_PREFIX;
    }
}
