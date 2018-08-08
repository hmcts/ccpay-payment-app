package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.contract.util.Method;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoorImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"embedded", "local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class PaymentRecordControllerTest {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoorImpl userRequestAuthorizer;

    @Autowired
    private PaymentDbBackdoor db;


    private static final String USER_ID = "1";

    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("dd-MM-yyyy");

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    private CheckDigit cd;

    @SneakyThrows
    private String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }

    private String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
    }


    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);
        cd = new LuhnCheckDigit();

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");

    }

    @Test
    public void testRecordCashPayment_withValidData() throws Exception {
        PaymentRecordRequest request = getPaymentRecordRequest(getCashPaymentPayload());

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertThat(response).isNotNull();
        assertThat(response.getPaymentGroupReference()).isNotNull();
        assertThat(response.getReference().matches(PAYMENT_REFERENCE_REFEX)).isEqualTo(true);
        assertThat(response.getStatus()).isEqualTo("Initiated");

        String reference = response.getReference().substring(3, response.getReference().length());
        assertThat(cd.isValid(reference.replace("-", ""))).isEqualTo(true);
    }

    private PaymentRecordRequest getPaymentRecordRequest(String payload) throws Exception{
        return objectMapper.readValue(payload.getBytes(), PaymentRecordRequest.class);
    }

    @Test
    public void testRecordPayment_withoutPaymentMethod() throws Exception {
        PaymentRecordRequest request = getPaymentRecordRequest(getPayloadWithNoCcdCaseNumberAndCaseReference());
        request.setReference("ref_123");

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("paymentMethod: must not be null");
    }

    @Test
    public void testRecordChequePayment_withoutReference() throws Exception {
        PaymentRecordRequest request = getPaymentRecordRequest(getPayloadWithNoCcdCaseNumberAndCaseReference());
        request.setPaymentMethod(Method.CHEQUE);

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("reference: must not be empty");
    }

    @Test
    public void testRecordCashPayment_withInvalidRequest() throws Exception {
        PaymentRecordRequest request = getPaymentRecordRequest(getPayloadWithNoCcdCaseNumberAndCaseReference());

        restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity());
    }

    private String getCashPaymentPayload() {

        return "{\n" +
            "  \"amount\": 32.19,\n" +
            "  \"payment_method\": \"CASH\",\n" +
            "  \"reference\": \"ref_123\",\n" +
            "  \"requestor\": \"DIGITAL_BAR\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"giro_slip_no\": \"12345\",\n" +
            "  \"site_id\": \"AA99\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 32.19,\n" +
            "      \"code\": \"FEE0123\",\n" +
            "      \"memo_line\": \"Bar Cash\",\n" +
            "      \"natural_account_code\": \"21245654433\",\n" +
            "      \"version\": \"1\",\n" +
            "      \"volume\": 1, \n" +
            "      \"reference\":  \"ref_123\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }


    private String getChequePaymentPayload() {

        return "{\n" +
            "  \"amount\": 99.99,\n" +
            "  \"payment_method\": \"CHEQUE\",\n" +
            "  \"requestor\": \"DIGITAL_BAR\",\n" +
            "  \"reference\": \"ref_122\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"external_provider\": \"cheque provider\",\n" +
            "  \"external_reference\": \"1000012\",\n" +
            "  \"giro_slip_no\": \"434567\",\n" +
            "  \"site_id\": \"AA001\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 99.99,\n" +
            "      \"code\": \"FEE0111\",\n" +
            "      \"reference\": \"ref_122\",\n" +
            "      \"version\": \"1\",\n" +
            "      \"volume\": 1\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String getPayloadWithNoCcdCaseNumberAndCaseReference() {

        return "{\n" +
            "  \"amount\": 32.19,\n" +
            "  \"service\": \"DIGITAL_BAR\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"giro_slip_no\": \"12345\",\n" +
            "  \"site_id\": \"AA99\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 32.19,\n" +
            "      \"code\": \"FEE0123\",\n" +
            "      \"memo_line\": \"Bar Cash\",\n" +
            "      \"natural_account_code\": \"21245654433\",\n" +
            "      \"version\": \"1\",\n" +
            "      \"volume\": 1\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
