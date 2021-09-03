package uk.gov.hmcts.payment.api.v1.componenttests;

import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityConfigurationComponentTest {

    private RestActions restActions;

    public void requestFromUnauthorizedServiceShouldResultIn403() throws Exception {
        restActions
            .withAuthorizedService("unauthorizedService")
            .post("/users/userId/payments/", "any")
            .andExpect(status().isForbidden());
    }

    public void requestToOtherUsersResourceShouldResultIn403() throws Exception {
        restActions
            .withAuthorizedService("divorce")
            .post("/users/otherUser/payments/", "any")
            .andExpect(status().isForbidden());
    }

    public void requestFromAuthorizedServiceAndUserShouldNotBe403() throws Exception {
        restActions
            .withAuthorizedService("cmc")
            .get("/users/userId/payments/123456")
            .andExpect(status().isNotFound());
    }

}
