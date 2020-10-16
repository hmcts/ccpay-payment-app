package uk.gov.hmcts.payment.api.componenttests;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.model.LegacySite;
import uk.gov.hmcts.payment.api.v1.componenttests.TestUtil;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class LegacyReferenceControllerTest extends TestUtil {

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;
    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;
    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;
    RestActions restActions;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withAuthorizedService("divorce");
    }

    @Test
    public void testFindAllPaymentLegacySites() throws Exception {
        restActions
            .get("/legacy-references/sites")
            .andExpect(status().isOk())
            .andExpect(body().asListOf(LegacySite.class, legacySites -> {
                assertThat(legacySites).anySatisfy(legacySite -> {
                    assertThat(legacySite.getSiteId()).isEqualTo("Y402");
                });
            }));
    }

}
