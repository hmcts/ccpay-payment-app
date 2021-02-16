package uk.gov.hmcts.payment.api.configuration;

import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FeatureToggleConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
public class FeatureToggleConfigurationTest {

    @Autowired
    LDConfig ldConfig;

    @Test
    public void givenPropertiesAllFieldsSetInLDConfig(){
        LDConfig mockldConfig =  new LDConfig.Builder()
            .http(
                Components.httpConfiguration()
                    .connectTimeout(Duration.ofSeconds(3))
                    .socketTimeout(Duration.ofSeconds(3))
            )
            .events(
                Components.sendEvents()
                    .flushInterval(Duration.ofSeconds(10))
            )
            .build();
        assertThat(ldConfig).isEqualToComparingFieldByFieldRecursively(mockldConfig);
    }

}
