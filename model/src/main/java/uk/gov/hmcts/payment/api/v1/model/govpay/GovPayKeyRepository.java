package uk.gov.hmcts.payment.api.v1.model.govpay;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toMap;

@Component
public class GovPayKeyRepository {
    private final Map<String, String> keys;

    @Autowired
    public GovPayKeyRepository(GovPayConfig govPayConfig) {
        this(govPayConfig.getKey());
    }

    public GovPayKeyRepository(Map<String, String> keys) {
        this.keys = keys
            .entrySet().stream()
            .collect(toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
    }

    public String getKey(String microservice) {
        return keys.get(microservice.toLowerCase());
    }
}
