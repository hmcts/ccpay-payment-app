package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.ResubmitRefundRemissionRequest;
import uk.gov.hmcts.payment.api.dto.RetroSpectiveRemissionRequest;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.service.PaymentRefundsServiceImpl;
import uk.gov.hmcts.payment.api.service.PaymentServiceImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NonPBAPaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

import java.math.BigDecimal;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RefundsControllerTest {

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;
    PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
        .paymentReference("RC-1234-1234-1234-1234")
        .refundReason("RESN1")
        .build();

    RetroSpectiveRemissionRequest retroSpectiveRemissionRequest = RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith()
        .remissionReference("qwerty").build();
    MockMvc mvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;
    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;
    @InjectMocks
    private RefundsController refundsController;
    @MockBean
    private PaymentRefundsServiceImpl paymentRefundsService;
    @MockBean
    private PaymentServiceImpl paymentService;
    private RestActions restActions;
    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

    }

    @After
    public void tearDown() {
        this.restActions = null;
        mvc = null;
    }

    @Test
    public void createRefundWithValidRequest() throws Exception {

        BigDecimal amount = new BigDecimal(100);
        ResponseEntity<RefundResponse> mockRefundResponse = new ResponseEntity<>(RefundResponse.RefundResponseWith()
            .refundReference("RF-4321-4321-4321-4321")
            .refundAmount(amount)
            .build(), HttpStatus.CREATED);


        when(paymentRefundsService.createRefund(any(), any())).thenReturn(mockRefundResponse);

        MvcResult result = restActions
            .post("/refund-for-payment", paymentRefundRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RefundResponse refundResponse = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RefundResponse.class);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getRefundReference());
        assertEquals(amount, refundResponse.getRefundAmount());

    }

    @Test
    public void createRefundWithInvalidRequestReturns404() throws Exception {

        when(paymentRefundsService.createRefund(any(), any())).thenThrow(new PaymentNotFoundException("reference not found"));

        restActions
            .post("/refund-for-payment", paymentRefundRequest)
            .andExpect(status().isNotFound());
    }

    @Test
    public void testRemissionAmountUpdateForResubmitRefundJourney() throws Exception {

        when(paymentRefundsService.updateTheRemissionAmount("RC-1111-2222-5555-2222",BigDecimal.valueOf(100), "RR036"))
            .thenReturn(new ResponseEntity(null, HttpStatus.OK));

       ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
           .resubmitRefundRemissionRequestWith()
           .amount(BigDecimal.valueOf(100))
           .refundReason("RR036")
           .build();

        restActions.
            patch(format("/refund/resubmit/RC-1111-2222-5555-2222"), resubmitRefundRemissionRequest)
            .andExpect(status().isOk())
            .andReturn();
    }


    @Test
    public void testRefundRequestExceptionForResubmitRefundJourney() throws Exception {

        when(paymentRefundsService.updateTheRemissionAmount("RC-1111-2222-5555-2222",BigDecimal.valueOf(100), "RR036"))
            .thenThrow(new InvalidRefundRequestException("Amount should not be more than Remission amount"));

        ResubmitRefundRemissionRequest resubmitRefundRemissionRequest = ResubmitRefundRemissionRequest
            .resubmitRefundRemissionRequestWith()
            .amount(BigDecimal.valueOf(100))
            .refundReason("RR036")
            .build();

        MvcResult result = restActions.
            patch(format("/refund/resubmit/RC-1111-2222-5555-2222"), resubmitRefundRemissionRequest)
            .andExpect(status().isBadRequest())
            .andReturn();

        Assertions.assertEquals("Amount should not be more than Remission amount", result.getResponse().getContentAsString());
    }

    @Test
    public void createRefundWithInvalidRequestReturns400() throws Exception {

        when(paymentRefundsService.createRefund(any(), any())).thenThrow(new InvalidRefundRequestException("Reference not valid"));
        restActions
            .post("/refund-for-payment", paymentRefundRequest)
            .andExpect(status().isBadRequest());
    }

    @Test
    public void createRetroRemissionRefundWithValidRequest() throws Exception {

        ResponseEntity<RefundResponse> mockRefundResponse = new ResponseEntity<>(RefundResponse.RefundResponseWith()
            .refundReference("RF-4321-4321-4321-4321")
            .build(), HttpStatus.CREATED);

        when(paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(any(), any())).thenReturn(mockRefundResponse);

        MvcResult result = restActions
            .post("/refund-retro-remission", retroSpectiveRemissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RefundResponse refundResponse = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RefundResponse.class);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getRefundReference());

    }


    @Test
    public void createRetroRemissionRefundWithInvalidRequestReturnsNonPbaPaymentException() throws Exception {

        when(paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(any(), any())).thenThrow(new NonPBAPaymentException("test 123"));

        MvcResult result = restActions
            .post("/refund-retro-remission", retroSpectiveRemissionRequest)
            .andExpect(status().isBadRequest())
            .andReturn();

    }

    @Test
    public void createRetroRemissionRefundWithInvalidRequestReturnsRemissionNotFoundException() throws Exception {

        when(paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(any(), any())).thenThrow(new RemissionNotFoundException("test 123"));

        MvcResult result = restActions
            .post("/refund-retro-remission", retroSpectiveRemissionRequest)
            .andExpect(status().isBadRequest())
            .andReturn();

    }

    @Test
    public void createRetroRemissionRefundWithInvalidRequestReturnsPaymentNotSuccessException() throws Exception {

        when(paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(any(), any())).thenThrow(new PaymentNotSuccessException("test 123"));

        MvcResult result = restActions
            .post("/refund-retro-remission", retroSpectiveRemissionRequest)
            .andExpect(status().isBadRequest())
            .andReturn();

    }
}
