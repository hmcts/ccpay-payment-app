package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.http.HttpStatus;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class PaymentGroupControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;


    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;

    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected PaymentDbBackdoor paymentDbBackdoor;

    @Autowired
    protected PaymentFeeDbBackdoor paymentFeeDbBackdoor;

    @Autowired
    private SiteService<Site, String> siteServiceMock;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");

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
    @Transactional
    public void retrievePaymentsRemissionsAndFeeByGroupReferenceTest() throws Exception {
        CardPaymentRequest cardPaymentRequest = getCardPaymentRequest();

        MvcResult result1 = restActions
            .withReturnUrl("https://www.google.com")
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();


        PaymentDto createPaymentResponseDto = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentDto.class);

        // Create a remission
        // Get fee id
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.findByReference(createPaymentResponseDto.getPaymentGroupReference());
        PaymentFee fee = paymentFeeDbBackdoor.findByPaymentLinkId(paymentFeeLink.getId());

        // create a partial remission
        MvcResult result2 = restActions
            .post("/payment-groups/" + createPaymentResponseDto.getPaymentGroupReference() + "/fees/" + fee.getId() + "/remissions", getRemissionRequest())
            .andExpect(status().isCreated())
            .andReturn();

        // Retrieve payment by payment group reference
        MvcResult result3 = restActions
            .get("/payment-groups/" + createPaymentResponseDto.getPaymentGroupReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result3.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        PaymentDto paymentDto = paymentGroupDto.getPayments().get(0);

        assertThat(paymentGroupDto).isNotNull();
        assertThat(paymentDto).isEqualToComparingOnlyGivenFields(cardPaymentRequest);
        assertThat(paymentDto.getReference()).isEqualTo(createPaymentResponseDto.getReference());
        assertThat(paymentDto.getDescription()).isEqualTo("description");
        assertThat(paymentDto.getCcdCaseNumber()).isEqualTo("1111-2222-3333-4444");
        assertThat(paymentDto.getServiceName()).isEqualTo("Probate");
        FeeDto feeDto = paymentGroupDto.getFees().stream().filter(f -> f.getCode().equals("FEE0123")).findAny().get();
        assertThat(feeDto.getCode()).isEqualTo("FEE0123");
        assertThat(feeDto.getVersion()).isEqualTo("1");
        assertThat(feeDto.getCalculatedAmount()).isEqualTo(new BigDecimal("550.00"));
        assertThat(feeDto.getNetAmount()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    public void retrievePaymentsAndFeesByPaymentGroupReferenceTest() throws Exception {
        CardPaymentRequest cardPaymentRequest = getCardPaymentRequest();

        MvcResult result1 = restActions
            .withReturnUrl("https://www.google.com")
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();


        PaymentDto createPaymentResponseDto = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentDto.class);

        // Retrieve payment by payment group reference
        MvcResult result3 = restActions
            .get("/payment-groups/" + createPaymentResponseDto.getPaymentGroupReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result3.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        PaymentDto paymentDto = paymentGroupDto.getPayments().get(0);

        assertThat(paymentGroupDto).isNotNull();
        assertThat(paymentDto).isEqualToComparingOnlyGivenFields(cardPaymentRequest);
        assertThat(paymentGroupDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(getFee());
    }

    @Test
    public void retrievePaymentsRemissionsAndFeesWithInvalidPaymentGroupReferenceShouldFailTest() throws Exception {
        restActions
            .get("/payment-groups/1011-10000000001")
            .andExpect(status().isNotFound());
    }

    private CardPaymentRequest getCardPaymentRequest() {
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("550.00"))
            .description("description")
            .ccdCaseNumber("1111-2222-3333-4444")
            .service(Service.PROBATE)
            .currency(CurrencyCode.GBP)
            .provider("pci pal")
            .channel("telephony")
            .siteId("AA001")
            .fees(Collections.singletonList(getFee()))
            .build();
    }

    private RemissionRequest getRemissionRequest() {
        return RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber("1111-2222-3333-4444")
            .hwfAmount(new BigDecimal("50.00"))
            .hwfReference("HR1111")
            .siteId("AA001")
            .fee(getFee())
            .build();
    }

    private FeeDto getFee() {
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber("1111-2222-3333-4444")
            .version("1")
            .code("FEE0123")
            .build();
    }
}
