package uk.gov.hmcts.payment.api.reports;

import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

public enum PaymentReportType {

    CARD,
    PBA_CMC,
    PBA_DIVORCE;

    public static PaymentReportType from(PaymentMethodType paymentMethodType, Service serviceType) {
        String value = serviceType != null ? String.join("_", paymentMethodType.name(), serviceType.name()) : paymentMethodType.name();
        try {
            return PaymentReportType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new  UnsupportedOperationException(String.format("No config defined as the report type is not supported for " +
                "paymentMethod %s and service %s ", paymentMethodType, serviceType));
        }
    }
}
