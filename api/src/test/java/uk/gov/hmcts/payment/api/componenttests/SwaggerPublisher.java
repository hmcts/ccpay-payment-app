package uk.gov.hmcts.payment.api.componenttests;

import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest", "mockcallbackservice"})
@SpringBootTest(webEnvironment = MOCK)
@AutoConfigureMockMvc
public class SwaggerPublisher {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @Before
    public void setup() {
        WebRequestTrackingFilter filter = new WebRequestTrackingFilter();
        filter.init(new MockFilterConfig()); // using a mock that you construct with init params and all
        this.mvc = webAppContextSetup(this.webAppContext)
            .apply(springSecurity())
            .addFilters(filter).build();
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateDocs() throws Exception {
        generateDocsForGroup("payment2");
        generateDocsForGroup("payment-external-api");
        generateDocsForGroup("reference-data");
    }

    private void generateDocsForGroup(String groupName) throws Exception {
        byte[] specs = mvc.perform(
            get("/v2/api-docs?group=" + groupName)
                .header("Authorization", "Bearer spoof")
        )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/swagger-specs." + groupName + ".json"))) {
            outputStream.write(specs);
        }
    }
}
