package uk.gov.hmcts.payment.functional;

import org.assertj.core.api.Assertions;
import org.ehcache.core.Ehcache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.payment.api.reports.FeesService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class FeeCacheFunctionalTest {

    @Autowired
    private FeesService feesService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void validateCache() throws  Exception {
        feesService.getFeesDtoMap(); // call the service.

        Cache cache = this.cacheManager.getCache("feesDtoMap");
        Map<String, Fee2Dto> feesDtoMap = (HashMap<String, Fee2Dto>)cache.get("getFeesDtoMap").get();

        assertThat(feesDtoMap).isNotNull();
        assertThat(feesDtoMap.size() > 0).isTrue();
        feesDtoMap.keySet().stream().forEach(k -> {
            assertThat(k.startsWith("FEE")).isTrue();
        });
    }
}
