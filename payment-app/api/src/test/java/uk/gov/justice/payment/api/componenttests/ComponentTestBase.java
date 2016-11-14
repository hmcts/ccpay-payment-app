package uk.gov.justice.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import net.logstash.logback.encoder.org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.payment.api.componenttests.backdoors.DbBackdoor;
import uk.gov.justice.payment.api.componenttests.sugar.CustomResultMatcher;
import uk.gov.justice.payment.api.componenttests.sugar.RestActions;

import java.io.IOException;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"embedded", "local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class ComponentTestBase {

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(9190);

    @Autowired
    protected DbBackdoor db;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    RestActions restActions;

    @Before
    public void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).build();
        this.restActions = new RestActions(mvc, objectMapper);
    }

    CustomResultMatcher bodyAs(Class expectedClass) {
        return new CustomResultMatcher(objectMapper, expectedClass);
    }

    CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    String contentsOf(String fileName) throws IOException {
        return IOUtils.toString(ResourceUtils.getURL("classpath:" + fileName).openStream());
    }

}