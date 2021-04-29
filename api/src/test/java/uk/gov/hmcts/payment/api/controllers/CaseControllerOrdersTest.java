package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.collections.map.MultiValueMap;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.componenttests.PaymentFeeDbBackdoor;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderFeeDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class CaseControllerOrdersTest extends PaymentsDataUtil {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    protected PaymentDbBackdoor paymentDbBackdoor;

    @Autowired
    protected PaymentFeeDbBackdoor paymentFeeDbBackdoor;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private SiteService<Site, String> siteServiceMock;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private ReferenceDataServiceImpl referenceDataService;

    @Autowired
    private FeesService feesService;

    private static final String USER_ID = UserResolverBackdoor.CASEWORKER_ID;

    @Autowired
    private ObjectMapper objectMapper;

    RestActions restActions;

    @Before
    public void setup() {
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

//    @Test
//    @Transactional
//    public void searchAllPaymentsWithCcdCaseNumberShouldReturnRequiredFieldsForVisualComponent() throws Exception {
//        createOrder("1");
//        MultiValueMap header = new MultiValueMap();
//        header.put("Key","Value");
//        OrderDto orderDto = OrderDto.orderDtoWith()
//                            .ccdCaseNumber("1607065108455502")
//                            .caseReference("caseReference")
//                            .caseType("case-type")
//                            .fees(Arrays.asList(OrderFeeDto.feeDtoWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
//                            .build();
//        MvcResult result = restActions
//                            .withAuthorizedUser(USER_ID)
//                            .withUserId(USER_ID)
//                            .post("/order",orderDto,header)
//                            .andExpect(status().isCreated())
//                            .andReturn();
//
//        OrderPaymentDto orderPaymentDto = OrderPaymentDto.paymentDtoWith()
//                                            .accountNumber("PBA-FUN-1234")
//                                            .description("description")
//                                            .ccdCaseNumber("1607065108455502")
//                                            .amount(new BigDecimal("99.99"))
//                                            .caseReference("caseReference")
//                                            .currency(CurrencyCode.GBP)
//                                            .customerReference("cust-ref")
//                                            .organisationName("Slater & Gordon498")
//                                            .service("divorce")
//                                            .build();
//        String orderReference = result.getResponse().getContentAsString();
//        MvcResult result1 = restActions
//                                .withAuthorizedUser(USER_ID)
//                                .withUserId(USER_ID)
//                                .post("/order/"+orderReference+"/credit-account-payment",orderPaymentDto)
//                                .andExpect(status().isCreated())
//                                .andReturn();
//        populateCardPaymentToDbForOrders("1");
//
//        MvcResult result2 = restActions
//            .withAuthorizedUser(USER_ID)
//            .withUserId(USER_ID)
//            .get("/orderpoc/cases/1607065108455502/payments")
//            .andExpect(status().isOk())
//            .andReturn();
//
//        PaymentsResponse payments = objectMapper.readValue(result2.getResponse().getContentAsByteArray(), new TypeReference<PaymentsResponse>(){});
//
//        assertThat(payments.getPayments().size()).isEqualTo(1);
//
//        PaymentDto payment = payments.getPayments().get(0);
//
//        assertThat(payment.getCcdCaseNumber()).isEqualTo("ccdCaseNumber1");
//
//        assertThat(payment.getReference()).isNotBlank();
//        assertThat(payment.getAmount()).isPositive();
//        assertThat(payment.getDateCreated()).isNotNull();
//        assertThat(payment.getCustomerReference()).isNotBlank();
//
//        Assert.assertThat(payment.getStatusHistories(), hasItem(hasProperty("status", is("Initiated"))));
//        Assert.assertThat(payment.getStatusHistories(), hasItem(hasProperty("errorCode", nullValue())));
//    }


}
