package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.TelephonyCallback;
import uk.gov.hmcts.payment.api.model.TelephonyRepository;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest", "mockcallbackservice"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class TelephonyControllerTest extends PaymentsDataUtil {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @MockBean
    protected CallbackServiceImpl callbackServiceImplMock;

    @Autowired
    protected PaymentDbBackdoor db;
    @Autowired
    private TelephonyRepository telephonyRepository;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, null, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    public void updateTelephonyPaymentStatusWithSuccess() throws Exception {
        String rawFormData = "orderCurrency=&orderAmount=488.50&orderReference=RC-1519-9028-1909-1435&ppAccountID=1210&" +
            "transactionResult=SUCCESS&transactionAuthCode=test123&transactionID=3045021106&transactionResponseMsg=&" +
            "avsAddress=&avsPostcode=&avsCVN=&cardExpiry=1220&cardLast4=9999&cardType=MASTERCARD&ppCallID=820782890&" +
            "customData1=MOJTest120190124123432&customData2=MASTERCARD&customData3=CreditCard&customData4=";

        String paymentReference = "RC-1519-9028-1909-1435";
        Payment dbPayment = populateTelephonyPaymentToDb(paymentReference, false);

        restActions
            .postWithFormData("/telephony/callback", rawFormData)
            .andExpect(status().isNoContent());

        MvcResult result = restActions
            .get("/payments?ccd_case_number=" + dbPayment.getCcdCaseNumber())
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertThat(payments.size()).isEqualTo(1);
        assertEquals(payments.get(0).getPaymentReference(), paymentReference);
        assertEquals("Success", payments.get(0).getStatus());

    }

    public void updateTelephonyPaymentStatus_recordinDBAndRaiseStatusCallbackEvent() throws Exception {
        String rawFormData = "orderCurrency=&orderAmount=488.50&orderReference=RC-1519-9028-1909-1435&ppAccountID=1210&" +
            "transactionResult=DECLINE&transactionAuthCode=test123&transactionID=3045021106&transactionResponseMsg=&" +
            "avsAddress=&avsPostcode=&avsCVN=&cardExpiry=1220&cardLast4=9999&cardType=MASTERCARD&ppCallID=820782890&" +
            "customData1=MOJTest120190124123432&customData2=MASTERCARD&customData3=CreditCard&customData4=";

        String paymentReference = "RC-1519-9028-1909-1435";
        Payment dbPayment = populateTelephonyPaymentToDb(paymentReference, true);

        restActions
            .postWithFormData("/telephony/callback", rawFormData)
            .andExpect(status().isNoContent());

        Optional<TelephonyCallback> optionalCallback = telephonyRepository.findById(paymentReference);
        assertThat(optionalCallback).isNotNull();

        verify(callbackServiceImplMock).callback(dbPayment.getPaymentLink(), dbPayment);
    }

    @Test
    public void updateTelephonyPaymentStatus_throw404IfPaymentNotFound() throws Exception {
        String rawFormData = "orderCurrency=&orderAmount=488.50&orderReference=RC-invalid-reference&ppAccountID=1210&" +
            "transactionResult=SUCCESS&transactionAuthCode=test123&transactionID=3045021106&transactionResponseMsg=&" +
            "avsAddress=&avsPostcode=&avsCVN=&cardExpiry=1220&cardLast4=9999&cardType=MASTERCARD&ppCallID=820782890&" +
            "customData1=MOJTest120190124123432&customData2=MASTERCARD&customData3=CreditCard&customData4=";

        String paymentReference = "RC-1519-9028-1909-1435";
        Payment dbPayment = populateTelephonyPaymentToDb(paymentReference, false);

        restActions
            .postWithFormData("/telephony/callback", rawFormData)
            .andExpect(status().isNotFound());

        verify(callbackServiceImplMock, times(0)).callback(dbPayment.getPaymentLink(), dbPayment);
    }

}
