package uk.gov.hmcts.payment.api.reports;

import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

public enum PaymentReportType {

    CARD,
    DIGITAL_BAR,
    PBA_CMC,
    PBA_DIVORCE,
    PBA_PROBATE,
    PBA_FINREM,
    PBA_FPL;

    public static PaymentReportType from(PaymentMethodType paymentMethodType, Service serviceType) {
        String value = "";
        if (paymentMethodType != null && serviceType != null) {
            value = String.join("_", paymentMethodType.name(), serviceType.name());
        } else if (serviceType != null) {
            value = serviceType.name();
        } else if (paymentMethodType != null) {
            value = paymentMethodType.name();
        }

        try {
            return PaymentReportType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedOperationException(String.format("No config defined as the report type is not supported for " +
                "paymentMethod %s and service %s ", paymentMethodType, serviceType));
        }
    }
}
