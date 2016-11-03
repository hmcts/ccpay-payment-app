package uk.gov.justice.payment.api.unit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@Configuration

public class CancelPaymentUnitTest extends AbstractPaymentTest {

    @Mock
    HttpServletRequest httpServletRequest;
    private String expectedViewPaymentResponse;
    private CreatePaymentRequest paymentRequest;

    @Before
    public void setUp() {
        try {
            super.setUp();
            when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/payments"));
            ReflectionTestUtils.setField(paymentController, "restTemplate", restTemplate);
            expectedViewPaymentResponse = new Scanner(new File(classLoader.getResource("viewPaymentResponse.json").getFile())).useDelimiter("\\Z").next();
            paymentRequest = new CreatePaymentRequest();
            paymentRequest.setAmount(10);
            paymentRequest.setDescription("Test Desc");
            paymentRequest.setPaymentReference("TestRef");
            paymentRequest.setServiceId("TEST_SERVICE");
            paymentRequest.setApplicationReference("Test case");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void cancelPayment() {
        stubFor(post(urlPathMatching("/payments/cancel/.*"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withBody(expectedViewPaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController, "url", URL + "/cancel");
        assertEquals(paymentController.cancelPayment(SERVICE_ID, null).getStatusCode().value(), 204);
    }

    @Test
    public void cancelPaymentFailed() {
        stubFor(post(urlPathMatching("/payments/cancel/failed/.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{ \"code\": \"P0501\", \"description\": \"Cancellation of payment failed\" }")
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController, "url", URL + "/cancel/failed");
        assertEquals(paymentController.cancelPayment(SERVICE_ID, null).getStatusCode().value(), 400);
    }

}
