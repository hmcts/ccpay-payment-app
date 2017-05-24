package uk.gov.hmcts.payment.api.model.govpay;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GovPayKeyRepositoryTest {

    private final GovPayKeyRepository repository = new GovPayKeyRepository(ImmutableMap.of("ID", "key"));

    @Test
    public void returnsKey() {
        assertThat(repository.getKey("id")).isEqualTo("key");
    }

    @Test
    public void returnsKeyCaseInsensitive() {
        assertThat(repository.getKey("iD")).isEqualTo("key");
    }
}
