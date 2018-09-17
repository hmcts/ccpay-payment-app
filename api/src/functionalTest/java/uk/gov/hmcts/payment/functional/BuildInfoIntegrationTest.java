package uk.gov.hmcts.payment.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;

public class BuildInfoIntegrationTest extends  IntegrationTestBase {

    @Autowired
    private PaymentsTestDsl scenario;


    @Test
    public void assertBuildInfoShouldBePresent() throws IOException, NoSuchFieldException {
        scenario.given()
            .when().getBuildInfo()
            .then().got(JsonNode.class, response -> {
            assertThat(response.at("/git/commit/id").asText()).isNotEmpty();
            assertThat(response.at("/build/version").asText()).isNotEmpty();
        });
    }
}
