package uk.gov.hmcts.payment.api.unit;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import lombok.SneakyThrows;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.payment.api.reports.FeesService;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@FixMethodOrder(MethodSorters.DEFAULT)
public class FeeCacheTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private FeesService feesService;

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        this.cacheManager.getCache("feesDtoMap").clear();
    }

    /**
     *
     * Test cached fees when fees-register returns
     * valid fees
     *
     * */
    @Test
    public void testCacheWithValidFees() throws  Exception {
        // Wire-mock fees-register response
        stubFor(get(urlPathMatching("/fees-register/fees"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("fees-register-responses/allfees.json"))));

        // Invoke fees-register service
        feesService.getFeesDtoMap();

        // Validate cached fees
        Cache cache = this.cacheManager.getCache("feesDtoMap");
        Map<String, Fee2Dto> feesDtoMap = (HashMap<String, Fee2Dto>)cache.get("getFeesDtoMap").get();

        assertThat(feesDtoMap).isNotNull();
        assertThat(feesDtoMap.size() > 0).isTrue();
        feesDtoMap.keySet().stream().forEach(k -> {
            assertThat(k.startsWith("FEE")).isTrue();
        });
    }

    /**
     *
     * Test cached fees when fees-register returns
     * no fees
     *
     * */
    @Test
    public void testCacheWithInValidFeesOrNoFees() throws Exception {
        // Wire-mock fees-register response
        stubFor(get(urlPathMatching("/fees-register/fees"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("")));

        // Invoke fees-register service
        feesService.getFeesDtoMap();

        // Validate cached fees
        Cache cache = this.cacheManager.getCache("feesDtoMap");
        assertThat(cache.get("getFeesDtoMap")).isNull();
    }

    @SneakyThrows
    protected String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }


    protected String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
    }
}
