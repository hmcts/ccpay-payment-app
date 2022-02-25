package uk.gov.hmcts.payment.api.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;

import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;


public class PciPalPaymentServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private PciPalPaymentService pciPalPaymentService;

    private String url = "";
    private String callbackUrl = "www.callback.url.com";

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        this.url = "http://localhost:" + wireMockRule.port();

        pciPalPaymentService = new PciPalPaymentService(
            url,
            callbackUrl,httpClient,objectMapper
        );
    }
}
