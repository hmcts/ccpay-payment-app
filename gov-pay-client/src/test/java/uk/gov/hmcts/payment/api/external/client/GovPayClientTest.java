package uk.gov.hmcts.payment.api.external.client;

import org.junit.Test;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;

public class GovPayClientTest {

    @Test
    public void test() {
        new CreatePaymentRequest(1, "", "", "");
    }
}
