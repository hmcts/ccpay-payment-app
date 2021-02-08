package uk.gov.hmcts.payment.functional;
import com.fasterxml.jackson.databind.JsonNode;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class BuildInfoIntegrationTest {

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
