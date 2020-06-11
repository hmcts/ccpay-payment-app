package uk.gov.hmcts.payment.api.configuration;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.stereotype.Service;

@Service
public class LaunchDarklyFeatureToggler implements FeatureToggler {

    private LDClientInterface ldClient;

    public LaunchDarklyFeatureToggler(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    public boolean getBooleanValue(String key, Boolean defaultValue) {

        LDUser user = new LDUser("user@test.com");

        return ldClient.boolVariation(
            key,
            user,
            defaultValue
        );
    }

}
