package uk.gov.hmcts.payment.api.componenttests;

import org.junit.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityConfigurationComponentTest extends ComponentTestBase {

    @Test
    public void requestFromUnauthorizedServiceShouldResultIn403() throws Exception {
        restActions
            .withAuthorizedUser("userId")
            .withAuthorizedService("unauthorizedService")
            .post("/users/userId/payments/", "any")
            .andExpect(status().isForbidden());
    }

    @Test
    public void requestToOtherUsersResourceShouldResultIn403() throws Exception {
        restActions
            .withAuthorizedUser("userId")
            .withAuthorizedService("divorce")
            .post("/users/otherUser/payments/", "any")
            .andExpect(status().isForbidden());
    }

    @Test
    public void requestFromAuthorizedServiceAndUserShouldNotBe403() throws Exception {
        restActions
            .withAuthorizedUser("userId")
            .withAuthorizedService("cmc")
            .get("/users/userId/payments/123456")
            .andExpect(status().isNotFound());
    }

}
