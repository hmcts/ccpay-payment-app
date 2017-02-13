package uk.gov.justice.payment.api.acceptancetests;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

@TestPropertySource(value = "classpath:config/application-test.properties", properties = "logging.level.org.springframework.web:ERROR")
public class TestBase {

    protected final ScenarioFactory scenario = new ScenarioFactory();
    public static String appBaseUri;

    @Value("${appBaseURL}")
    public void setappURL(String app_base_uri) {
        appBaseUri = app_base_uri;
    }

    public static Properties CONFIG = null;

    @BeforeClass
    public static void initialize() throws IOException {
        InputStream is = TestBase.class.getClassLoader().getResourceAsStream("config/config.properties");
        CONFIG = new Properties();
        CONFIG.load(is);
    }


    protected static String getProcessedTemplateValue(final String fileName,
                                                      final Map<String, String> templateValueMap) {
        try {
            return getProcessedTemplateValue(ResourceUtils.getURL("classpath:" + fileName).openStream(), templateValueMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String getProcessedTemplateValue(final InputStream template,
                                                      final Map<String, String> templateValueMap) {
        VelocityContext context = new VelocityContext(templateValueMap);
        StringWriter swOut = new StringWriter();
        Velocity.evaluate(context, swOut, "Log template", new InputStreamReader(template));
        return swOut.toString();
    }

    public static class ScenarioFactory {
        public RequestSpecification given() {
            return RestAssured.given()
                    .baseUri(appBaseUri)
                    .basePath("/payments")
                    .contentType("application/json")
                    .header(CONFIG.getProperty("k_service_id"), CONFIG.getProperty("service_id"));
        }
    }
}
