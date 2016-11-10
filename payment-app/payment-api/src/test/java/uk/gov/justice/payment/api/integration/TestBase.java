package uk.gov.justice.payment.api.integration;

import com.google.common.io.CharStreams;
import io.restassured.specification.RequestSpecification;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.junit.BeforeClass;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;

public class TestBase {

    public static Properties CONFIG = null;

    @BeforeClass
    public static void initialize() throws IOException {
        CONFIG = new Properties();
        InputStream is = TestBase.class.getClassLoader().getResourceAsStream("Config/config.properties");
        CONFIG.load(is);

    }

    // This function used for passing parameters using velocity template
    protected static String getProcessedTemplateValue(final String templateString,
                                                      final Map<String, String> templateValueMap) {
        String retValue = null;
        if (templateString == null) {
            return retValue;
        }
        VelocityContext context = new VelocityContext();
        if (templateValueMap != null && !templateValueMap.isEmpty()) {
            Iterator<String> templateValueMapIter = templateValueMap.keySet().iterator();
            while (templateValueMapIter.hasNext()) {
                String key = templateValueMapIter.next();
                Object value = templateValueMap.get(key);
                context.put(key, value);
            }
        }
        StringWriter swOut = new StringWriter();

        Velocity.evaluate(context, swOut, "Log template", templateString);
        retValue = swOut.toString();

        return retValue;
    }


    protected static String loadFile(String filename) {
        try {
            return CharStreams.toString(new InputStreamReader(PaymentsHappyPath.class.getClassLoader().getResourceAsStream(filename)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected RequestSpecification givenValidRequest() {
        return given()
                .baseUri(CONFIG.getProperty("baseURL"))
                .basePath("/payments")
                .contentType("application/json")
                .header(CONFIG.getProperty("k_service_id"), CONFIG.getProperty("service_id"));
    }

}
