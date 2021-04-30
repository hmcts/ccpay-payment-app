package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.collections.map.MultiValueMap;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
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
import uk.gov.hmcts.payment.api.domain.service.CaseDetailsDomainService;
import uk.gov.hmcts.payment.api.domain.service.FeeDomainService;
import uk.gov.hmcts.payment.api.domain.service.PaymentDomainService;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderFeeDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.exception.CaseDetailsNotFoundException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
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

    @MockBean
    private CaseDetailsDomainService caseDetailsDomainService;

    @MockBean
    private FeeDomainService feeDomainService;

    @MockBean
    private PaymentDomainService paymentDomainService;

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
    }

    @Test
    public void testGettingPaymentGroupDetailsWithValidCcdCaseNumber() throws Exception {

        when(caseDetailsDomainService.findByCcdCaseNumber(anyString())).thenReturn(getCaseDetails());

        when(feeDomainService.getFeePayApportionsByFee(Mockito.any(PaymentFee.class))).thenReturn(Arrays.asList(getFeePayApportion()));

        when(paymentDomainService.getPaymentByApportionment(Mockito.any(FeePayApportion.class))).thenReturn(getPayment());

        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/1607065108455502/paymentgroups")
            .andExpect(status().isOk())
            .andReturn();
        PaymentGroupResponse paymentGroupResponse = objectMapper.readValue(result2.getResponse().getContentAsString(),PaymentGroupResponse.class);

        BigDecimal actualAmount = paymentGroupResponse.getPaymentGroups().get(0).getPayments().get(0).getAmount();
        String actualFeeCode = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getCode();

        assertEquals(BigDecimal.valueOf(99.99) ,actualAmount);
        assertEquals("FEE0001",actualFeeCode);
    }

    @Test
    public void testGettingPaymentGroupDetailsWithInValidCcdCaseNumberSendsBadRequest() throws Exception {
        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/11/paymentgroups")
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testGettingPaymentGroupWithUnavailableCcdCaseNumber_ThrowCaseDetailsNotFoundException() throws Exception {
        when(caseDetailsDomainService.findByCcdCaseNumber(anyString())).thenThrow(new CaseDetailsNotFoundException("Case Details Not found for 1607065108000002"));

        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/1607065108000002/paymentgroups")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void  testGettingEmptyPaymentGroupThrowPaymentNotFoundException() throws Exception{
        CaseDetails caseDetails = getCaseDetails();
        caseDetails.setOrders(Collections.<PaymentFeeLink>emptySet());
        when(caseDetailsDomainService.findByCcdCaseNumber(anyString())).thenReturn(caseDetails);
        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/1607065108000002/paymentgroups")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void testGettingPaymentDetailsWithInValidCcdCaseNumberSendsBadRequest() throws Exception {
        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/11/paymentgroups")
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testGettingPaymentWithUnavailableCcdCaseNumber_ThrowCaseDetailsNotFoundException() throws Exception {
        when(caseDetailsDomainService.findByCcdCaseNumber(anyString())).thenThrow(new CaseDetailsNotFoundException("Case Details Not found for 1607065108000002"));

        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/1607065108000002/paymentgroups")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void  testGettingEmptyPaymentThrowPaymentNotFoundException() throws Exception{
        CaseDetails caseDetails = getCaseDetails();
        caseDetails.setOrders(Collections.<PaymentFeeLink>emptySet());
        when(caseDetailsDomainService.findByCcdCaseNumber(anyString())).thenReturn(caseDetails);
        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/1607065108000002/payments")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void testGettingPaymentDetailsWithValidCcdCaseNumber() throws Exception {

        when(caseDetailsDomainService.findByCcdCaseNumber(anyString())).thenReturn(getCaseDetails());

        when(feeDomainService.getFeePayApportionsByFee(Mockito.any(PaymentFee.class))).thenReturn(Arrays.asList(getFeePayApportion()));

        when(paymentDomainService.getPaymentByApportionment(Mockito.any(FeePayApportion.class))).thenReturn(getPayment());

        MvcResult result2 = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/orderpoc/cases/1607065108455502/payments")
            .andExpect(status().isOk())
            .andReturn();
        PaymentsResponse paymentsResponse = objectMapper.readValue(result2.getResponse().getContentAsString(),PaymentsResponse.class);

        BigDecimal actualAmount = paymentsResponse.getPayments().get(0).getAmount();
        String actualCaseReference = paymentsResponse.getPayments().get(0).getCaseReference();

        assertEquals(BigDecimal.valueOf(99.99) ,actualAmount);
        assertEquals("Reference1",actualCaseReference);
    }


    private FeePayApportion getFeePayApportion(){
        return FeePayApportion.feePayApportionWith()
            .apportionAmount(new BigDecimal("99.99"))
            .apportionType("AUTO")
            .feeId(1)
            .paymentId(1)
            .feeAmount(new BigDecimal("99.99"))
            .paymentId(1)
            .paymentLink(getPaymentFeeLink())
            .build();
    }

    private PaymentFeeLink getPaymentFeeLink(){
        return PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .orgId("org-id")
            .enterpriseServiceName("enterprise-service-name")
            .paymentReference("payment-ref")
            .ccdCaseNumber("1607065108455502")
            .fees(Arrays.asList(PaymentFee.feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .build();
    }

    private CaseDetails getCaseDetails(){
        return CaseDetails.caseDetailsWith()
            .id(1)
            .ccdCaseNumber("1607065108455502")
            .dateCreated(DateUtil.convertToDateViaInstant(LocalDateTime.now()))
            .dateUpdated(DateUtil.convertToDateViaInstant(LocalDateTime.now()))
            .orders(Arrays.asList(getPaymentFeeLink()).stream().collect(Collectors.toSet()))
            .build();
    }

    private Payment getPayment(){
        return Payment.paymentWith()
            .id(1)
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference1")
            .ccdCaseNumber("1607065108455502")
            .description("Test payments statuses  ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA09")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-0001")
            .build();
    }

}
