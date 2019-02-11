package uk.gov.hmcts.payment.api.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


public class PciPalPaymentServiceTest {


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private PciPalPaymentService pciPalPaymentService;

    @Before
    public void setUp() throws Exception {
        pciPalPaymentService = new PciPalPaymentService(
            "http://localhost:" + wireMockRule.port(),
            "apiKey",
            HttpClients.createMinimal()
        );
    }
    @Ignore
    @Test
    public void sendToPciPal() throws UnsupportedEncodingException {
        stubFor(
            post(urlEqualTo("http://localhost:" + wireMockRule.port()))
                .withRequestBody(
                    new EqualToJsonPattern("{ \"apiKey\": \"apiKey\", \"ppAccountId\": \"1234\", \"renderMethod\": \"IFRAME\", \"orderAmount\": \"200\" ,\"orderCurrency\": \"GBP\", \"orderReference\": \"RC12345678\"}", true, false)
                )
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBodyFile("pci-pal-response.txt")
                )
        );

        PciPalPaymentRequest request = PciPalPaymentRequest.pciPalPaymentRequestWith().apiKey("apiKey").orderAmount("200").orderCurrency("GBP").ppAccountID("1234")
            .orderReference("orderReference").callbackURL("http://example.com").renderMethod("HTML").build();
        String url = "http://example.com";
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("apiKey", "apiKey"));
        params.add(new BasicNameValuePair("ppAccountId", "1234"));
        params.add(new BasicNameValuePair("renderMethod", "HTML"));
        params.add(new BasicNameValuePair("orderAmount", "200"));
        params.add(new BasicNameValuePair("orderCurrency", "GBP"));
        params.add(new BasicNameValuePair("orderReference", "RC123456789"));
        params.add(new BasicNameValuePair("callbackURL", "http://example.com"));
        String  response = pciPalPaymentService.sendInitialPaymentRequest(request,"cmc");
        assertThat(response).isEqualTo("http://example.com");
    }




}
