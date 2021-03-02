package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class PbaControllerTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    PaymentService<PaymentFeeLink, String> paymentService;

    private static final String USER_ID = UserResolverBackdoor.SOLICITOR_ID;

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    public void shouldReturnPaymentsForAccount() throws Exception {
        PaymentFeeLink paymentFeeLink = getPaymentFeeLink();
        List<PaymentFeeLink> paymentFeeLinkList = new ArrayList<>();
        paymentFeeLinkList.add(paymentFeeLink);
        when(paymentService.search(Mockito.any(PaymentSearchCriteria.class))).thenReturn(paymentFeeLinkList);
        MvcResult mvcResult = restActions
            .get("/pba-accounts/123123213/payments")
            .andExpect(status().isOk())
            .andReturn();
    }

    private PaymentFeeLink getPaymentFeeLink(){
        List<Remission> remissionList = new ArrayList<>();
        Remission remission1 = Remission.remissionWith()
            .remissionReference("remission-reference")
            .build();
        remissionList.add(remission1);
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("499.99")).version("1").code("X0123").build();
        List<PaymentFee> paymentFees = new ArrayList<>();
        paymentFees.add(fee);
        List<Payment> payments = new ArrayList<>();
        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("121.11"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA011")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .externalReference("ah0288ctvgqgcmbatdp1viu61j")
            .reference("RC-1529-9159-9129-3183")
            .statusHistories(Arrays.asList(statusHistory))
            .build();
        payments.add(payment);
        return PaymentFeeLink.paymentFeeLinkWith()
            .remissions(remissionList)
            .payments(payments)
            .paymentReference("payment-reference")
            .fees(paymentFees)
            .build();
    }
}
