package uk.gov.hmcts.payment.api.service.govpay;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.exceptions.PaymentServiceNotFoundException;

import java.util.HashMap;
import java.util.Map;

@Component
public class ServiceToTokenMap {
    private static final Map<String, String> servicesMap = new HashMap<>();

    static {
        servicesMap.put("divorce", "divorce_frontend");
        servicesMap.put("probate", "probate_frontend");
        servicesMap.put("civil money claims", "cmc");
        servicesMap.put("specified money claims", "cmc");
    }

    public String getServiceKeyVaultName(String serviceName) {
        if(servicesMap.get(serviceName.toLowerCase()) != null) {
            return servicesMap.get(serviceName.toLowerCase());
        } else {
            throw new PaymentServiceNotFoundException(serviceName + " service not found in map");
        }
    }
}
