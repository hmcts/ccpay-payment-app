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
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;
import uk.gov.justice.payment.api.services.PaymentService;

import static org.mockito.Mockito.*;
import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


@RunWith(SpringRunner.class)
@SpringBootTest
@Configuration

public class CreatePaymentUnitTest extends AbstractPaymentTest{

    public static final int PORT = 9092;
    public static final String URL = "http://localhost:"+PORT+"/payments";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    protected PaymentController paymentController = new PaymentController();
    protected ObjectMapper mapper = new ObjectMapper();
    protected RestTemplate restTemplate = new RestTemplate();
    protected ClassLoader classLoader = getClass().getClassLoader();

    private String expectedCreatePaymentResponse;


    private CreatePaymentRequest paymentRequest;
    @Mock
    HttpServletRequest httpServletRequest;

    @Before
    public void setUp() throws FileNotFoundException {

        MockitoAnnotations.initMocks(this);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/payments"));
        ReflectionTestUtils.setField(paymentController,"restTemplate",restTemplate);
        ReflectionTestUtils.setField(paymentController,"mapper",mapper);

        expectedCreatePaymentResponse = new Scanner(new File(classLoader.getResource("createPaymentResponse.json").getFile())).useDelimiter("\\Z").next();

        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription("Test Desc");
        paymentRequest.setPaymentReference("TestRef");
        paymentRequest.setServiceId("TEST_SERVICE");
        paymentRequest.setApplicationReference("Test case");
    }
    @Test
    public void createPaymentSuccess() {

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController,"url", URL +"/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", new PaymentService() {
            public void storePayment(CreatePaymentRequest request, CreatePaymentResponse response) {}
        });
        assertEquals(paymentController.createPayment(paymentRequest,httpServletRequest).getStatusCode().value(), 201);
    }

    @Test
    public void createPaymentAuthenticationFailure() {
        stubFor(post(urlPathMatching("/payments/create_fail"))
                .willReturn(aResponse()
                        .withStatus(401)

                ));
        ReflectionTestUtils.setField(paymentController,"url",URL+"/create_fail");
        assertEquals(paymentController.createPayment(paymentRequest,httpServletRequest).getStatusCode().value(), 401);
    }




}
