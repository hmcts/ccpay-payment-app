package uk.gov.justice.payment.api.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.PaymentController;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@SpringBootTest
@Configuration

public class ViewPaymentUnitTest extends AbstractPaymentTest{

    public static final int PORT = 9092;
    public static final String URL = "http://localhost:"+PORT+"/payments";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);
    PaymentController paymentController = new PaymentController();
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    ClassLoader classLoader = getClass().getClassLoader();

    private String expectedCreatePaymentResponse;

    private String expectedViewPaymentResponse;

    private CreatePaymentRequest paymentRequest;
    @Mock
    HttpServletRequest httpServletRequest;

    @Before
    public void setUp() throws FileNotFoundException {

        MockitoAnnotations.initMocks(this);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/payments"));
        ReflectionTestUtils.setField(paymentController,"restTemplate",restTemplate);
        ReflectionTestUtils.setField(paymentController,"mapper",mapper);

        expectedViewPaymentResponse = new Scanner(new File(classLoader.getResource("viewPaymentResponse.json").getFile())).useDelimiter("\\Z").next();

        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription("Test Desc");
        paymentRequest.setPaymentReference("TestRef");
        paymentRequest.setServiceId("TEST_SERVICE");
        paymentRequest.setApplicationReference("Test case");
    }



    @Test
    public void viewPayment() {
        stubFor(get(urlPathMatching("/payments/view/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(expectedViewPaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController,"url",URL+"/view");
        assertEquals(paymentController.viewPayment(null).getStatusCode().value(), 200);
   }




    @Test
    public void paymentNotFound() {
        stubFor(get(urlPathMatching("/payments/view_not_found/.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody(expectedViewPaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController,"url",URL+"/view_not_found");
        assertEquals(paymentController.viewPayment(null).getStatusCode().value(), 404);
    }



}
