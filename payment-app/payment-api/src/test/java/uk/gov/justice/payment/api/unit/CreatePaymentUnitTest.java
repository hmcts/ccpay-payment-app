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

public class CreatePaymentUnitTest extends AbstractPaymentTest {


    public static final String TEST_SERVICE = "TEST_SERVICE";
    public static final int TEST_AMOUNT = 10;
    public static final String TEST_REF = "TestRef";
    public static final String TEST_DESC = "Test Desc";
    public static final String TEST_CASE = "Test Case";
    public static final String RETURN_URL = "https://local";
    //private static final String SERVICE_ID = "test-service-id";
    @Mock
    HttpServletRequest httpServletRequest;
    private String expectedCreatePaymentResponse;
    private CreatePaymentRequest paymentRequest;

    @Before
    public void setUp() {
        try {
            super.setUp();
            when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/payments"));
            doNothing().when(paymentService).updatePayment(null, null);
            ReflectionTestUtils.setField(paymentController, "restTemplate", restTemplate);
            expectedCreatePaymentResponse = new Scanner(new File(classLoader.getResource("createPaymentResponse.json").getFile())).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void createPaymentValidationMissingAmount() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }


    @Test
    public void createPaymentValidationMissingDesription() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(TEST_AMOUNT);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationMissingPaymentReference() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationBlankPaymentReference() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference("");
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationMissingServiceId() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(null, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationBlankServiceId() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId("");
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment("", paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationMissingApplicationReference() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setReturnUrl(URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationBlankApplicationReference() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setReturnUrl("https://local");
        paymentRequest.setApplicationReference("");

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationMissingReturnUrl() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationBlankReturnUrl() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl("");

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationNonHttpsReturnUrl() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl("http://local");

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentSuccess() {
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        //paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl("https://local");

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "url", URL + "/create");
        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 201);
    }

    @Test
    public void createPaymentAuthenticationFailure() {

        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setServiceId(TEST_SERVICE);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl("https://local");

        stubFor(post(urlPathMatching("/payments/create_fail"))
                .willReturn(aResponse()
                        .withStatus(401)

                ));
        ReflectionTestUtils.setField(paymentController, "url", URL + "/create_fail");
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest, httpServletRequest).getStatusCode().value(), 401);
    }


}
