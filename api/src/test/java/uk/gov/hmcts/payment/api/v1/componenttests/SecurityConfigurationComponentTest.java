package uk.gov.hmcts.payment.api.v1.componenttests;

import org.junit.Test;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityConfigurationComponentTest extends TestUtil {

    private static final ResultMatcher UNAUTHORISED_OR_FORBIDDEN = result -> {
        int statusCode = result.getResponse().getStatus();
        if (statusCode != 401 && statusCode != 403) {
            throw new AssertionError("Expected HTTP 401 or 403 but got " + statusCode);
        }
    };

    @Test
    public void unauthenticatedPostToPaymentFailuresShouldBeRejected() throws Exception {
        restActions
            .post("/payment-failures/bounced-cheque", null)
            .andExpect(UNAUTHORISED_OR_FORBIDDEN);
    }

    @Test
    public void unauthenticatedPatchToPaymentFailuresShouldBeRejected() throws Exception {
        restActions
            .patch("/payment-failures/some-failure-reference", null)
            .andExpect(UNAUTHORISED_OR_FORBIDDEN);
    }

    @Test
    public void requestFromUnauthorizedServiceShouldResultIn403() throws Exception {
        restActions
            .withAuthorizedService("unauthorizedService")
            .post("/users/userId/payments/", "any")
            .andExpect(status().isForbidden());
    }

    @Test
    public void requestToOtherUsersResourceShouldResultIn403() throws Exception {
        restActions
            .withAuthorizedService("divorce")
            .post("/users/otherUser/payments/", "any")
            .andExpect(status().isForbidden());
    }

    @Test
    public void requestFromAuthorizedServiceAndUserShouldNotBe403() throws Exception {
        restActions
            .withAuthorizedService("cmc")
            .get("/users/userId/payments/123456")
            .andExpect(status().isNotFound());
    }
}
