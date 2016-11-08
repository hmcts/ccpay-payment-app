package uk.gov.justice.payment.api.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.payment.api.controllers.dto.CreatePaymentRequestDto;

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


    public static final int TEST_AMOUNT = 10;
    public static final String TEST_REF = "TestRef";
    public static final String TEST_DESC = "Test Desc";
    public static final String TEST_CASE = "Test Case";
    public static final String RETURN_URL = "https://local";
    //private static final String SERVICE_ID = "test-service-id";
    @Mock
    HttpServletRequest httpServletRequest;
    private String expectedCreatePaymentResponse;
    private CreatePaymentRequestDto paymentRequest;

    @Before
    public void setUp() {
        try {
            super.setUp();
            when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8181/payments"));
            doNothing().when(paymentService).updatePayment(null, null);
            expectedCreatePaymentResponse = new Scanner(new File(classLoader.getResource("createPaymentResponse.json").getFile())).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void createPaymentValidationMissingAmount() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
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


        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }


    @Test
    public void createPaymentValidationMissingDesription() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(TEST_AMOUNT);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));


        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationMissingPaymentReference() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));


        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationBlankPaymentReference() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference("");
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl(RETURN_URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));


        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationMissingApplicationReference() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setReturnUrl(URL);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));


        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationBlankApplicationReference() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setReturnUrl("https://local");
        paymentRequest.setApplicationReference("");

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationMissingReturnUrl() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setApplicationReference(TEST_CASE);

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationBlankReturnUrl() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl("");

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));


        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }

    @Test
    public void createPaymentValidationNonHttpsReturnUrl() throws JsonProcessingException {
        paymentRequest = new CreatePaymentRequestDto();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription(TEST_DESC);
        paymentRequest.setPaymentReference(TEST_REF);
        paymentRequest.setApplicationReference(TEST_CASE);
        paymentRequest.setReturnUrl("http://local");

        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expectedCreatePaymentResponse)
                        .withHeader("Content-Type", "application/json")
                ));


        ReflectionTestUtils.setField(paymentController, "paymentService", paymentService);
        assertEquals(paymentController.createPayment(SERVICE_ID, paymentRequest).getStatusCode().value(), 422);
    }
}
