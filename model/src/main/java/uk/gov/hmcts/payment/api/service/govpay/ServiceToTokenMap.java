package uk.gov.hmcts.payment.api.service.govpay;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ServiceToTokenMap {
    private static final Map<String, String> servicesMap = new HashMap<>();

    static {
        servicesMap.put("Divorce", "divorce_frontend");
        servicesMap.put("Probate", "probate_frontend");
        servicesMap.put("Civil Money Claims", "cmc");
        servicesMap.put("Specified Money Claims", "cmc");
    }

    public String getServiceKeyVaultName(String serviceName) {
        return servicesMap.get(serviceName);
    }
}
