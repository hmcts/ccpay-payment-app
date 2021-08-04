package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.service.PaymentRefundsServiceImpl;
import uk.gov.hmcts.payment.api.service.PaymentServiceImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

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
public class RefundControllerTest {

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;
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
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

    }

    @Test
    public void createRefundWithValidRequest() throws Exception {

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("RC-1234-1234-1234-1234")
            .refundReason("RESN1")
            .build();

        RefundResponse mockRefundResponse = RefundResponse.RefundResponseWith().refundReference("RF-4321-4321-4321-4321").build();

        when(paymentRefundsService.CreateRefund(any(), any())).thenReturn(mockRefundResponse);

        MvcResult result = restActions
            .post("/refund-for-payment", paymentRefundRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RefundResponse refundResponse = objectMapper.readValue(result.getResponse().getContentAsByteArray(),RefundResponse.class);

        assertEquals("RF-4321-4321-4321-4321", refundResponse.getRefundReference());

    }

    @Test
    public void createRefundWithInvalidRequestReturns404() throws Exception {

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("1234-1234-1234-1234")
            .refundReason("RESN1")
            .build();

        when(paymentRefundsService.CreateRefund(any(), any())).thenThrow(new PaymentNotFoundException("reference not found"));

        restActions
            .post("/refund-for-payment", paymentRefundRequest)
            .andExpect(status().isNotFound());
    }


    @Test
    public void createRefundWithInvalidRequestReturns504() throws Exception {

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("1234-1234-1234-1234")
            .refundReason("RESN1")
            .build();

        when(paymentRefundsService.CreateRefund(any(), any())).thenThrow(new GatewayTimeoutException("Gateway timeout"));
        restActions
            .post("/refund-for-payment", paymentRefundRequest)
            .andExpect(status().isGatewayTimeout());
    }

    @Test
    public void createRefundWithInvalidRequestReturns400() throws Exception {

        PaymentRefundRequest paymentRefundRequest = PaymentRefundRequest.refundRequestWith()
            .paymentReference("1234-1234-1234-1234")
            .refundReason("RESN1")
            .build();

        when(paymentRefundsService.CreateRefund(any(), any())).thenThrow(new InvalidRefundRequestException("Reference not valid"));
        restActions
            .post("/refund-for-payment", paymentRefundRequest)
            .andExpect(status().isBadRequest());
    }
}
