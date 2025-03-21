package uk.gov.hmcts.payment.api.reports;

import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.util.HashMap;

public enum PaymentReportType {

    DUPLICATE_PAYMENT,
    CARD,
    PBA_DIVORCE,
    PBA_PROBATE,
    PBA_FINREM,
    PBA_FPL,
    PBA_CIVIL,
    PBA_PRL,
    PBA_IAC,
    PBA_SMC;

    public static PaymentReportType from(PaymentMethodType paymentMethodType, String serviceType) {
        String value = "";

        String serviceTypeEnum = getServiceTypeEnum(serviceType);

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
        HashMap<String, String> serviceTypeHashMap = new HashMap<>();


        serviceTypeHashMap.put("Financial Remedy", "FINREM");
        serviceTypeHashMap.put("Finrem", "FINREM");
        serviceTypeHashMap.put("Family Public Law", "FPL");
        serviceTypeHashMap.put("Divorce", "DIVORCE");
        serviceTypeHashMap.put("Probate","PROBATE");
        serviceTypeHashMap.put("Civil","CIVIL");
        serviceTypeHashMap.put("Damages","CIVIL");
        serviceTypeHashMap.put("Family Private Law","PRL");
        serviceTypeHashMap.put("Immigration and Asylum Appeals", "IAC");
        serviceTypeHashMap.put("Specified Money Claims", "SMC");


        return serviceTypeHashMap.getOrDefault(serviceType, serviceType);
    }
}
