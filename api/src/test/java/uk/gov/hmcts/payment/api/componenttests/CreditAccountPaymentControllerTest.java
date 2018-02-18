package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"embedded", "local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class CreditAccountPaymentControllerTest {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    protected PaymentDbBackdoor db;


    private static final String USER_ID = "user-id";

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @SneakyThrows
    String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }

    String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
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
    }


    @Test
    public void createCreditAccountPaymentTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    public void retrieveCreditAccountPaymentByPaymentGroupReference() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);

        MvcResult createResponse = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated())
            .andReturn();
        PaymentGroupDto paymentGroup = objectMapper.readValue(createResponse.getResponse().getContentAsByteArray(), PaymentGroupDto.class);

        MvcResult result = restActions
            .get(format("/credit-account/payments/" + paymentGroup.getPaymentGroupReference()))
            .andExpect(status().isOk())
            .andReturn();
        PaymentGroupDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        assertNotNull(response);
        assertEquals(response.getPaymentGroupReference(), paymentGroup.getPaymentGroupReference());
        response.getPayments().stream().forEach(p -> {
            assertTrue(p.getReference().matches(PAYMENT_REFERENCE_REFEX));
        });

        BigDecimal paymentsTotalAmount = response.getPayments().stream().map(PaymentDto::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feesTotalAmount = response.getFees().stream().map(FeeDto::getCalculatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(paymentsTotalAmount, feesTotalAmount);
    }

    @Test
    public void validCreditAccountPaymentRequestJsonTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);
        assertNotNull(request);
    }


    private String creditAccountPaymentRequestJson() {
        return "{\n" +
            "  \"payments\": [\n" +
            "    {\n" +
            "      \"amount\": 8000.00,\n" +
            "      \"description\": \"description1\",\n" +
            "      \"ccd_case_number\": \"ccdCaseNo1\",\n" +
            "      \"case_reference\": \"caseRef1\",\n" +
            "      \"service_name\": \"Divorce\",\n" +
            "      \"currency\": \"GBP\",\n" +
            "      \"customer_reference\": \"R1234567890\",\n" +
            "      \"organisation_name\": \"myOrganisation\",\n" +
            "      \"pba_number\": \"pba001\",\n" +
            "      \"site_id\": \"AA001\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"amount\": 500.00,\n" +
            "      \"description\": \"description2\",\n" +
            "      \"ccd_case_number\": \"ccdCaseNo1\",\n" +
            "      \"case_reference\": \"caseRef1\",\n" +
            "      \"service_name\": \"Divorce\",\n" +
            "      \"currency\": \"GBP\",\n" +
            "      \"customer_reference\": \"R1234567890\",\n" +
            "      \"organisation_name\": \"myOrganisation\",\n" +
            "      \"pba_number\": \"pba001\",\n" +
            "      \"site_id\": \"AA001\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"amount\": 1500.00,\n" +
            "      \"description\": \"description1\",\n" +
            "      \"ccd_case_number\": \"ccdCaseNo1\",\n" +
            "      \"case_reference\": \"caseRef1\",\n" +
            "      \"service_name\": \"Divorce\",\n" +
            "      \"currency\": \"GBP\",\n" +
            "      \"customer_reference\": \"R1234567890\",\n" +
            "      \"organisation_name\": \"myOrganisation\",\n" +
            "      \"pba_number\": \"pba001\",\n" +
            "      \"site_id\": \"AA001\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"code\": \"X0123\",\n" +
            "      \"version\": \"1\",\n" +
            "      \"calculated_amount\": 10000.00\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
