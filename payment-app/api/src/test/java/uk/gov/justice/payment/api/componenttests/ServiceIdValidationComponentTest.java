package uk.gov.justice.payment.api.componenttests;

import org.junit.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ServiceIdValidationComponentTest extends ComponentTestBase {

    @Test
    public void unknownServiceIdShouldResultIn422() throws Exception {
        restActions.get("/payments/?payment_reference=any", headersForServiceId("divorce"))
                .andExpect(status().isOk());

        restActions.get("/payments/?payment_reference=any", headersForServiceId("unknown"))
                .andExpect(status().isUnprocessableEntity());
    }

    private HttpHeaders headersForServiceId(String unknown) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("service_id", unknown);
        return httpHeaders;
    }
}