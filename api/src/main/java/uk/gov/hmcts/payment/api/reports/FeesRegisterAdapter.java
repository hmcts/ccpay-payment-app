package uk.gov.hmcts.payment.api.reports;

import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;

import javax.transaction.Transactional;

import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
@Transactional
public class FeesRegisterAdapter {

    private static final Logger LOG = getLogger(FeesRegisterAdapter.class);

    private FeesRegisterClient feesRegisterClient;

    public FeesRegisterAdapter(FeesRegisterClient feesRegisterClient) {
        this.feesRegisterClient = feesRegisterClient;

    }

    @Cacheable(value = "feesDtoMap", key = "#root.method.name", unless = "#result == null || #result.isEmpty()")
    public Map<String, Fee2Dto> getFeesDtoMap() {
        Map<String, Fee2Dto> feesDtoMap = null;
        try {
            Optional<Map<String, Fee2Dto>> optionalFeesDtoMap = feesRegisterClient.getFeesDataAsMap();
            if (optionalFeesDtoMap.isPresent()) {
                feesDtoMap = optionalFeesDtoMap.get();
            }
        } catch (Exception ex) {
            LOG.error("FeesService  -  Unable to get fees data. {}", ex.getMessage());
        }

        return feesDtoMap;
    }
}
