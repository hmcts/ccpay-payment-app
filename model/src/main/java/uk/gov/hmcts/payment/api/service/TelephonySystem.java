package uk.gov.hmcts.payment.api.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class representing a telephony system.
 * This class provides the structure for different telephony systems
 * and defines methods to retrieve flow IDs based on service types.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class TelephonySystem {
    private String systemName;
    private String grantType;
    private String tenantName;
    private String clientId;
    private String clientSecret;
    private String tokensURL;
    private String launchURL;
    private String viewIdURL;
    private String strategicFlowId;
    private String probateFlowId;
    private String divorceFlowId;
    private String prlFlowId;
    private String iacFlowId;
    public static final String DEFAULT_SYSTEM_NAME = "kerv";

    public String getFlowId(String serviceType) {
        Map<String, String> flowIdMap = new HashMap<>();
        flowIdMap.put("Probate", this.getProbateFlowId());
        flowIdMap.put("Divorce", this.getDivorceFlowId());
        flowIdMap.put("Specified Money Claims", this.getStrategicFlowId());
        flowIdMap.put("Financial Remedy", this.getStrategicFlowId());
        flowIdMap.put("Family Private Law", this.getPrlFlowId());
        flowIdMap.put("Immigration and Asylum Appeals", this.getIacFlowId());

        if (!flowIdMap.containsKey(serviceType)) {
            throw new PaymentException("This telephony system does not support telephony calls for the service '" + serviceType + "'.");
        }
        return flowIdMap.get(serviceType);
    }
}
