package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.payment.api.v1.componenttests.ComponentTestBase;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class CardPaymentComponentTest extends ComponentTestBase {

    private static final String USER_ID = "userId";

    RestActions restActions;

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Before
    public void setup() {
        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID);
    }

    public void createCardPaymentTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/GOV_PAY_ID"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(contentsOf("gov-pay-responses/status-created.json"))
                .withHeader("Content-Type", "application/json")
            ));


    }

    @SneakyThrows
    String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }

    String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
    }
}
