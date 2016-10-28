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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@Configuration

public class ViewPaymentUnitTest extends AbstractPaymentTest {


    @Mock
    HttpServletRequest httpServletRequest;
    private String expectedCreatePaymentResponse;
    private String expectedViewPaymentResponse;
    private CreatePaymentRequest paymentRequest;

    @Before
    public void setUp() {
        try {
            super.setUp();
            when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/payments"));
            doNothing().when(paymentService).storePayment(null, null);
            ReflectionTestUtils.setField(paymentController, "restTemplate", restTemplate);
            ReflectionTestUtils.setField(paymentController, "mapper", mapper);
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
    public void viewPayment() {
        stubFor(get(urlPathMatching("/payments/view/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(expectedViewPaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController, "url", URL + "/view");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.viewPayment(null, null).getStatusCode().value(), 200);
    }


    @Test
    public void paymentNotFound() {
        stubFor(get(urlPathMatching("/payments/view_not_found/.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody(expectedViewPaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController, "url", URL + "/view_not_found");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.viewPayment(null, null).getStatusCode().value(), 404);
    }


}
