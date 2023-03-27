package uk.gov.hmcts.payment.api.v1.model.govpay;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(GovPayKeyRepository.class);

    public GovPayKeyRepository(Map<String, String> keys) {
        this.keys = keys
            .entrySet().stream()
            .collect(toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
    }

    public String getKey(String microservice) {
        keys.forEach((key,val)->{
            LOG.info("key {}",key);
            LOG.info("val {}",val);
        });
        LOG.info("microservice.toLowerCase() {}",microservice.toLowerCase());
        return keys.get(microservice.toLowerCase());
    }
}
