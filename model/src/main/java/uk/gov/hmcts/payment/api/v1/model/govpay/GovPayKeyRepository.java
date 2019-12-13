package uk.gov.hmcts.payment.api.v1.model.govpay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Component
public class GovPayKeyRepository {
    private final Map<String, String> keys;
    private static final Logger LOG = LoggerFactory.getLogger(GovPayKeyRepository.class);

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
        LOG.debug("GovPayKeyRepository : KEY : {} ---> VALUE : {}", microservice, keys.get(microservice.toLowerCase()));
        return keys.get(microservice.toLowerCase());
    }
}
