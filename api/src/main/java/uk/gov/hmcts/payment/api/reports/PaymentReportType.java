package uk.gov.hmcts.payment.api.reports;

import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.util.HashMap;

public enum PaymentReportType {

    CARD,
    DIGITAL_BAR,
    PBA_CMC,
    PBA_DIVORCE,
    PBA_PROBATE,
    PBA_FINREM,
    PBA_FPL,
    PBA_CIVIL,
    PBA_PRL;

    public static PaymentReportType from(PaymentMethodType paymentMethodType, String serviceType) {
        String value = "";

        String serviceTypeEnum = getServiceTypeEnum2(serviceType);

        if (paymentMethodType != null && serviceTypeEnum != null) {
            value = String.join("_", paymentMethodType.name(), serviceTypeEnum);
        } else if (serviceTypeEnum != null) {
            value = serviceTypeEnum;
        } else if (paymentMethodType != null) {
            value = paymentMethodType.name();
        }

        try {
            return PaymentReportType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedOperationException(String.format("No config defined as the report type is not supported for " +
                "paymentMethod %s and service %s ", paymentMethodType, serviceTypeEnum));
        }
    }

    private static String getServiceTypeEnum(String serviceType) {
        if (serviceType != null && (serviceType.equalsIgnoreCase("Specified Money Claims") || serviceType.equalsIgnoreCase("Civil Money Claims"))) {
            return "CMC";
        }
        if (serviceType != null && (serviceType.equalsIgnoreCase("Financial Remedy") || serviceType.equalsIgnoreCase("Finrem"))) {
            return "FINREM";
        }
        if (serviceType != null && (serviceType.equalsIgnoreCase("Family Public Law"))) {
            return "FPL";
        }
        if (serviceType != null && (serviceType.equalsIgnoreCase("Digital Bar"))) {
            return "DIGITAL_BAR";
        }
        if (serviceType != null && (serviceType.equalsIgnoreCase("Divorce"))) {
            return "DIVORCE";
        }
        if (serviceType != null && (serviceType.equalsIgnoreCase("Probate"))) {
            return "PROBATE";
        }
        if (serviceType != null && (serviceType.equalsIgnoreCase("Civil"))) {
            return "CIVIL";
        }
        if (serviceType != null && (serviceType.equalsIgnoreCase("Family Private Law"))) {
            return "PRL";
        }
        return serviceType;
    }
    private static String getServiceTypeEnum2(String ServiceType) {
        HashMap<String, String> serviceTypeHashMap = new HashMap<>();

        serviceTypeHashMap.put("Specified Money Claims", "CMC");
        serviceTypeHashMap.put("Civil Money Claims", "CMC");
        serviceTypeHashMap.put("Financial Remedy", "FINREM");
        serviceTypeHashMap.put("Finrem", "FINREM");
        serviceTypeHashMap.put("Family Public Law", "FPL");
        serviceTypeHashMap.put("Digital Bar", "DIGITAL_BAR");
        serviceTypeHashMap.put("Divorce", "DIVORCE");
        serviceTypeHashMap.put("Probate","PROBATE");
        serviceTypeHashMap.put("Civil","CIVIL");
        serviceTypeHashMap.put("Family Private Law","PRL");

        if (ServiceType != null) {
            return serviceTypeHashMap.get(ServiceType);
        }

//        Throw some error here?
        return "invalid ServiceType";
    }
}
