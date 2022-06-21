package uk.gov.hmcts.payment.api.componenttests.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.microsoft.azure.servicebus.IMessage;
import org.ff4j.FF4j;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.controllers.MaintenanceJobsController;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.CasePaymentRequest;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public class MaintenanceJobsControllerTest extends PaymentsDataUtil {

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;
    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);
    @Rule
    public WireMockClassRule instanceRule = wireMockRule;
    MockMvc mvc;
    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Value("${service.callback.url}")
    private String serviceCallbackUrl;
    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;
    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;
    @Autowired
    private PaymentDbBackdoor db;
    private RestActions restActions;
    @MockBean
    private TopicClientProxy topicClientProxy;
    @MockBean
    private FF4j ff4j;
    @InjectMocks
    private MaintenanceJobsController controller;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    ServiceRequestDomainService serviceRequestDomainServiceMock;

    @Before
    public void setUp() {

        mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        when(ff4j.check(CallbackService.FEATURE)).thenReturn(true);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");
    }

    @After
    public void tearDown() {
        this.restActions = null;
    }

    @Test
    public void testThatServiceCallbackIsInvokedWhenAStatusChangeIsDetectedOnGovPay() throws Exception {

        // setup

        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response.json"))));

        stubFor(get(urlPathMatching("/v1/payments/ak8gtvb438drmp59cs7ijppr3i"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-response-created.json"))));


        // Create Payment

        MvcResult result = restActions
            .withHeader("service-callback-url", serviceCallbackUrl)
            .post("/card-payments", cardPaymentRequest())
            .andExpect(status().isCreated())
            .andReturn();


        // Run status update => service callback is not invoked

        restActions.
            patch("/jobs/card-payments-status-update", null)
            .andExpect(status().isOk());

        verify(topicClientProxy, times(0)).send(any(IMessage.class));

        // Update status in gov pay

        stubFor(get(urlPathMatching("/v1/payments/ak8gtvb438drmp59cs7ijppr3i"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-response.json"))));

        // Run status update => service callback is invoked

        restActions.
            patch("/jobs/card-payments-status-update", null)
            .andExpect(status().isOk());

        verify(topicClientProxy, times(1)).send(any(IMessage.class));


        // Run status update again => Delegating service is not called

        restActions.
            patch("/jobs/card-payments-status-update", null)
            .andExpect(status().isOk());

        verify(topicClientProxy, times(1)).send(any(IMessage.class));

    }

    /*@Test
    public void testThatCallbackIsInvokedWhenAStatusChangeIsDetectedOnGovPay() throws Exception {

        // setup

        stubFor(post(urlPathMatching("/v1/payments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(contentsOf("gov-pay-responses/create-payment-response.json"))));

        stubFor(get(urlPathMatching("/v1/payments/ak8gtvb438drmp59cs7ijppr3i"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(contentsOf("gov-pay-responses/get-payment-response-created.json"))));

        // Create a Service Request
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
                .caseReference("123245677")
                .hmctsOrgId("MoneyClaimCase")
                .ccdCaseNumber("8689869686968696")
                .fees(getMultipleFees())
                .callBackUrl("http://callback/url")
                .casePaymentRequest(getCasePaymentRequest())
                .build();
        ServiceRequestResponseDto serviceRequestResponseDto
                = ServiceRequestResponseDto.serviceRequestResponseDtoWith().serviceRequestReference("2020-1234567890123").build();
        when(serviceRequestDomainServiceMock.create(serviceRequestDto, any())).thenReturn(serviceRequestResponseDto);

        // Create Payment
        MvcResult result = restActions
                .withHeader("service-callback-url", "")
                .post("/card-payments", cardPaymentRequest())
                .andExpect(status().isCreated())
                .andReturn();

        // Run status update => service callback is not invoked

        restActions.
                patch("/jobs/card-payments-status-update", null)
                .andExpect(status().isOk());

        verify(topicClientProxy, times(0)).send(any(IMessage.class));

        // Update status in gov pay

        stubFor(get(urlPathMatching("/v1/payments/ak8gtvb438drmp59cs7ijppr3i"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(contentsOf("gov-pay-responses/get-payment-response.json"))));

        // Run status update => service callback is invoked

        restActions.
                patch("/jobs/card-payments-status-update", null)
                .andExpect(status().isOk());

        verify(topicClientProxy, times(1)).send(any(IMessage.class));


        // Run status update again => Delegating service is not called

        restActions.
                patch("/jobs/card-payments-status-update", null)
                .andExpect(status().isOk());

        verify(topicClientProxy, times(1)).send(any(IMessage.class));

    }*/

    private CardPaymentRequest cardPaymentRequest() throws Exception {
        return objectMapper.readValue(requestJson().getBytes(), CardPaymentRequest.class);
    }

    private List<ServiceRequestFeeDto> getMultipleFees() {
        ServiceRequestFeeDto fee1 = ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("100"))
                .code("FEE100")
                .version("1")
                .volume(1)
                .build();

        ServiceRequestFeeDto fee2 = ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("200"))
                .code("FEE102")
                .version("1")
                .volume(1)
                .build();

        return Arrays.asList(fee1, fee2);
    }

    private CasePaymentRequest getCasePaymentRequest(){
        CasePaymentRequest casePaymentRequest = CasePaymentRequest.casePaymentRequestWith()
                .action("action")
                .responsibleParty("party")
                .build();

        return casePaymentRequest;
    }
}
