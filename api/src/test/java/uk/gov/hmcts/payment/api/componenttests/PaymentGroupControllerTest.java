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
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
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

    }

    @Test
    @Transactional
    public void testRetrievePaymentsByGroupReference() throws Exception {
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("100.1"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber("1234")
            .service(Service.PROBATE)
            .currency(CurrencyCode.GBP)
            .provider("pci pal")
            .channel("telephony")
            .siteId("siteId")
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .code("FEE0001")
                .version("1")
                .calculatedAmount(new BigDecimal("100.1"))
                .build()))
            .build();

        MvcResult result1 = restActions
            .withReturnUrl("https://www.google.com")
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentDto.class);
        String reference = paymentDto.getReference();

        // Retrieve payment by payment group reference
        MvcResult result2 = restActions
            .get("/payment-group/" + paymentDto.getPaymentGroupReference())
            .andExpect(status().isOk())
            .andReturn();

        paymentDto = objectMapper.readValue(result2.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertThat(paymentDto).isNotNull();
        assertThat(paymentDto.getReference()).isEqualTo(reference);
        assertThat(paymentDto.getDescription()).isEqualTo("description");
        assertThat(paymentDto.getCcdCaseNumber()).isEqualTo("1234");
        assertThat(paymentDto.getServiceName()).isEqualTo("Probate");
        FeeDto feeDto = paymentDto.getFees().stream().filter(f -> f.getCode().equals("FEE0001")).findAny().get();
        assertThat(feeDto.getCode()).isEqualTo("FEE0001");
        assertThat(feeDto.getVersion()).isEqualTo("1");
        assertThat(feeDto.getCalculatedAmount()).isEqualTo(new BigDecimal("100.1"));

    }
}
