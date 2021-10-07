package uk.gov.hmcts.payment.api.reports.config;

import uk.gov.hmcts.payment.api.dto.PaymentDto;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

public interface PaymentReportConfig {

    PaymentReportType getType();

    String getFrom();

    String[] getTo();

    String getSubject();

    String getMessage();

    boolean isEnabled();

    String getCsvFileNamePrefix();

    String getCsvHeader();

    String getCsvRecord(PaymentDto paymentDto);

}
