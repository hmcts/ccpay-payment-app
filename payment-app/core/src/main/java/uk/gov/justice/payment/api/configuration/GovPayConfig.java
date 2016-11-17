package uk.gov.justice.payment.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "gov.pay.auth")
public class GovPayConfig {
    private Map<String, String> key;

    public Map<String, String> getKey() {
        return key;
    }

    public void setKey(Map<String, String> key) {
        this.key = key;
    }

    public boolean hasKeyForService(String serviceId) {
        return key.containsKey(serviceId);
    }

    public String getKeyForService(String serviceId) {
        return key.get(serviceId);
    }
}