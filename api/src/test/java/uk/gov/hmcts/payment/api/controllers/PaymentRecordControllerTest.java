package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.PaymentRecordService;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class PaymentRecordControllerTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    PaymentProviderRepository paymentProviderRespository;

    @MockBean
    SiteService<Site, String> siteServiceMock;

    @MockBean
    PaymentRecordService<PaymentFeeLink, String> paymentRecordService;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    RestActions restActions;

    @Before
    public void setup(){
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);
        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");
        List<Site> serviceReturn = Arrays.asList(Site.siteWith()
                .sopReference("sop")
                .siteId("AA99")
                .name("name")
                .service("service")
                .id(1)
                .build(),
            Site.siteWith()
                .sopReference("sop")
                .siteId("AA001")
                .name("name")
                .service("service")
                .id(1)
                .build()
        );

        when(siteServiceMock.getAllSites()).thenReturn(serviceReturn);
    }

    @Test
    public void testPostPaymentRecord() throws Exception {
        PaymentRecordRequest paymentRecordRequest = getCashPaymentRequest();
        PaymentProvider mockPaymentProvider = PaymentProvider.paymentProviderWith()
                                                    .name("middle office provider")
                                                    .description("description")
                                                    .build();
        when(paymentProviderRespository.findByNameOrThrow(any(String.class))).thenReturn(mockPaymentProvider);
        List<Payment> paymentList = new ArrayList<>();
        Payment payment = Payment.paymentWith()
                            .paymentStatus(PaymentStatus.SUCCESS)
                            .reference("reference")
                            .dateCreated(Date.valueOf("2020-01-02"))
                            .build();
        paymentList.add(payment);
        PaymentFeeLink mockPaymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
                                                .id(1)
                                                .paymentReference("2021-1614597111984")
                                                .dateCreated(Date.valueOf("2020-01-02"))
                                                .dateUpdated(Date.valueOf("2020-01-03"))
                                                .payments(paymentList)
                                                .build();
        when(paymentRecordService.recordPayment(any(Payment.class), any(List.class), anyString())).thenReturn(mockPaymentFeeLink);
        MvcResult result = restActions
            .post("/payment-records", paymentRecordRequest)
            .andExpect(status().isCreated())
            .andReturn();
        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentDto.class);
        assertEquals("reference",response.getReference());

    }

    @Test
    public void testPostPaymentRecord_ThrowPaymentException() throws Exception {
        PaymentRecordRequest paymentRecordRequest = getCashPaymentRequest();
        paymentRecordRequest.setSiteId("invalid-site-id");
        MvcResult result = restActions
            .post("/payment-records", paymentRecordRequest)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    private PaymentRecordRequest getCashPaymentRequest() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("32.19"))
            .paymentMethod(PaymentMethodType.CASH)
            .reference("ref_123")
            .externalProvider("middle office provider")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .giroSlipNo("12345")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA99")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("32.19"))
                        .code("FEE0123")
                        .memoLine("Bar Cash")
                        .naturalAccountCode("21245654433")
                        .version("1")
                        .volume(1)
                        .reference("ref_123")
                        .build()
                )
            )
            .build();
    }
}
