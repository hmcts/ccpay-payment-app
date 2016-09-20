package uk.gov.justice.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.RestAssured;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.payment.api.PaymentController;
import uk.gov.justice.payment.api.json.CreatePaymentRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;


@RunWith(SpringRunner.class)
@SpringBootTest
@Configuration
@TestPropertySource(properties="gov.pay.url=https://publicapi.integration.pymnt.uk/v1/payments")

public class PaymentApiApplicationUnitTest {

	@Value("${gov.pay.url}")
	private String url;

	@Rule
	public  WireMockRule wireMockRule = new WireMockRule(8089);
	@Test
	public void createPayment() {
		String expected = "{  \"amount\":10,  \"state\":{    \"status\":\"created\",    \"finished\":false }}";
		stubFor(post(urlPathMatching("/payments/create"))
				.willReturn(aResponse()
						.withStatus(201)
						.withBody(expected)
						.withHeader("Content-Type", "application/json")
						));

		PaymentController paymentController = new PaymentController();
		paymentController.url = "http://localhost:8089/payments/create";
		assertEquals(paymentController.createPayment(null,null).getStatusCode().value(), 201);



	}


	@Test
	public void viewPayment() {
		String expected = "{   \"amount\":3650,    \"state\":{         \"status\":\"success\",       \"finished\":true    }     }";
		stubFor(get(urlPathMatching("/payments/view/.*"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody(expected)
						.withHeader("Content-Type", "application/json")
				));

		PaymentController paymentController = new PaymentController();
		paymentController.url = "http://localhost:8089/payments/view";
		assertEquals(paymentController.viewPayment(null,null).getStatusCode().value(), 200);



	}

	private String getJson(Object obj) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return "";
	}
}
