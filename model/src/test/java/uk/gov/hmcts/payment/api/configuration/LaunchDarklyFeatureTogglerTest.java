package uk.gov.hmcts.payment.api.configuration;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LaunchDarklyFeatureTogglerTest {

    @Mock
    private LDClientInterface ldClient;


    @InjectMocks
    private LaunchDarklyFeatureToggler launchDarklyFeatureToggler;

    @Test
    public void should_return_default_value_when_key_does_not_exist() {
        String notExistingKey = "not-existing-key";
        LDUser user = new LDUser("user@test.com");
        when(ldClient.boolVariation(
            notExistingKey,user,
            true)
        ).thenReturn(true);

        assertTrue(launchDarklyFeatureToggler.getBooleanValue(notExistingKey, true));
    }

    @Test
    public void should_return_value_when_key_exists() {
        String existingKey = "existing-key";
        LDUser user = new LDUser("user@test.com");
        when(ldClient.boolVariation(
            existingKey,user,
            false)
        ).thenReturn(true);

        assertTrue(launchDarklyFeatureToggler.getBooleanValue(existingKey, false));
    }

}
