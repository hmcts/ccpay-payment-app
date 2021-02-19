package uk.gov.hmcts.payment.api.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class GovPayClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private GovPayClient client;

    @Before
    public void setUp() throws Exception {
        client = new GovPayClient(
            "http://localhost:" + wireMockRule.port(),
            HttpClients.createMinimal(),
            new ObjectMapper(),
            new GovPayErrorTranslator(new ObjectMapper())
        );
    }

    @Test
    public void createPayment() {
        stubFor(
            post(urlEqualTo("/"))
                .withHeader("Authorization", matching("Bearer token"))
                .withRequestBody(
                    new EqualToJsonPattern("{ \"amount\": 12000, \"reference\": \"reference\", \"description\": \"description\", \"return_url\": \"return_url\", \"language\": \"language\"}", true, false)
                )
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBodyFile("gov-pay-response-created.json")
                )
        );

        CreatePaymentRequest request = new CreatePaymentRequest(12000, "reference", "description", "return_url","language");
        GovPayPayment payment = client.createPayment("token", request);
        assertThat(payment.getAmount()).isEqualTo(12000);
    }


    @Test
    public void retrievePayment() {
        stubFor(
            get(urlEqualTo("/someId"))
                .withHeader("Authorization", matching("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBodyFile("gov-pay-response-created.json")
                )
        );

        GovPayPayment payment = client.retrievePayment("token", "someId");
        assertThat(payment.getAmount()).isEqualTo(12000);
    }

    @Test
    public void cancelPayment() {
        stubFor(
            post(urlEqualTo("/cancel"))
                .withHeader("Authorization", matching("Bearer token"))
                .willReturn(aResponse().withStatus(204))
        );

        client.cancelPayment("token", "http://localhost:" + wireMockRule.port() + "/cancel");
    }

    @Test(expected = GovPayPaymentNotFoundException.class)
    public void govPayErrorHandling() {
        stubFor(
            post(urlEqualTo("/cancel"))
                .withHeader("Authorization", matching("Bearer token"))
                .willReturn(aResponse().withStatus(400).withBody("{  \"code\": \"P0500\", \"description\": \"Message explaining the error\"}"))
        );

        client.cancelPayment("token", "http://localhost:" + wireMockRule.port() + "/cancel");
    }

    @Test
    public void RefundPayment() {
        stubFor(
            post(urlEqualTo("/refund"))
                .withHeader("Authorization", matching("Bearer token"))
                .willReturn(aResponse().withStatus(204))
        );

        RefundPaymentRequest request = new RefundPaymentRequest(12000, 200);
        client.refundPayment("token", "http://localhost:" + wireMockRule.port() + "/refund",request);
    }
}
