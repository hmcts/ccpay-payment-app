package uk.gov.hmcts.payment.api.reports.config;

import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.DuplicatePaymentDto;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

public interface PaymentReportConfig<T> {

    PaymentReportType getType();

    String getFrom();

    String[] getTo();

    String getSubject();

    String getMessage();

    boolean isEnabled();

    String getCsvFileNamePrefix();

    String getCsvHeader();

    String getCsvRecord(T dto);

}
