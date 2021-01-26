package uk.gov.hmcts.payment.api.reports;

import uk.gov.hmcts.payment.api.util.PaymentMethodType;

public enum PaymentReportType {

    CARD,
    DIGITAL_BAR,
    PBA_CMC,
    PBA_DIVORCE,
    PBA_PROBATE,
    PBA_FINREM,
    PBA_FPL;

    public static PaymentReportType from(PaymentMethodType paymentMethodType, String serviceType) {
        String value = "";

        serviceType = getServiceType(serviceType);

        if (paymentMethodType != null && serviceType != null) {
            value = String.join("_", paymentMethodType.name(), serviceType);
        } else if (serviceType != null) {
            value = serviceType;
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

    private static String getServiceType(String serviceType) {
        if(serviceType != null && (serviceType.equalsIgnoreCase("Civil Money Claims") || serviceType.equalsIgnoreCase("Specified Money Claims"))) {
            serviceType = "CMC";
        }
        if(serviceType != null && (serviceType.equalsIgnoreCase("Finrem") || serviceType.equalsIgnoreCase("Financial Remedy"))) {
            serviceType = "FINREM";
        }
        if(serviceType != null && (serviceType.equalsIgnoreCase("Family Public Law"))) {
            serviceType = "FPL";
        }
        if(serviceType != null && (serviceType.equalsIgnoreCase("Digital Bar"))) {
            serviceType = "DIGITAL_BAR";
        }
        return serviceType;
    }
}
