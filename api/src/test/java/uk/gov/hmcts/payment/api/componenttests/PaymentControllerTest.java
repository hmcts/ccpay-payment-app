package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.core.JsonParser;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.UpdatePaymentRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.fasterxml.jackson.core.JsonParser.*;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(SpringRunner.class)
@ActiveProfiles({"embedded", "local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class PaymentControllerTest {

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

    private final static String PAYMENT_METHOD = "payment by account";

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
    public void updateCaseReference_forGivenPaymentReferenceTest() throws Exception {
        //Create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("caseReference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Description1")
            .serviceType("Probate")
            .currency("GBP")
            .siteId("AA01")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-3890")
            .build();
        Fee fee = Fee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186168000").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result1 = restActions.
            get(format("/credit-account-payments/RC-1519-9028-1909-3890"))
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertEquals(paymentDto.getCaseReference(), "caseReference");
        assertEquals(paymentDto.getCcdCaseNumber(), "ccdCaseNumber");

        /* -- update payment -- */
        UpdatePaymentRequest updatePaymentRequest = objectMapper.readValue(updatePaymentRequestJson().getBytes(), UpdatePaymentRequest.class);
        MvcResult result2 = restActions.
            patch(format("/payments/" + paymentDto.getReference()), updatePaymentRequest)
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @Test
    public void upateCaseReferenceValidation_forEmptyValues() throws Exception{
        UpdatePaymentRequest updatePaymentRequest = objectMapper.readValue(updatePaymentInvalidRequestJson().getBytes(), UpdatePaymentRequest.class);
        MvcResult result = restActions.
            patch(format("/payments/RC-1519-9028-1909-3111"), updatePaymentRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), "eitherOneEmpty: Either ccdCaseNumber or caseReference cannot be empty.");
    }


    private String updatePaymentRequestJson() {
        return "{\n" +
            "  \"case_reference\": \"newCaseReference\",\n" +
            "  \"ccd_case_number\": \"newCcdCaseNumber\"\n" +
            "}";
    }


    private String updatePaymentInvalidRequestJson() {
        return "{\n" +
            "  \"case_reference\": \"\",\n" +
            "  \"ccd_case_number\": \"newCcdCaseNumber\"\n" +
            "}";
    }

}
