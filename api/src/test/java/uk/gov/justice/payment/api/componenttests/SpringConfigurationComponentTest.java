package uk.gov.justice.payment.api.componenttests;

import org.junit.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SpringConfigurationComponentTest extends ComponentTestBase {

    @Test
    public void invalidRequestShouldResultIn400() throws Exception {
        restActions.post("/payments/", "{ invalid json }").andExpect(status().isBadRequest());
    }
}