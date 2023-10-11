package uk.gov.hmcts.payment.functional;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.CasePaymentRequest;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class ServiceRequestPaymentFunctionalTest {

    @Autowired
    private TestConfigProperties testProps;
    @Autowired
    private PaymentsTestDsl dsl;
    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;
    @Autowired
    private PaymentTestService paymentTestService;
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void createAnServiceRequestAndMakePBAPayment() {
        String ccdCaseNumber = "11116464" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        UUID randomUUID = UUID.randomUUID();
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .hmctsOrgId("ABA1")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("abcd-defg-hjik-1234")
            .casePaymentRequest(getCasePaymentRequest())
            .callBackUrl("http://callback.hmcts.net")
            .fees(Arrays.asList(ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(BigDecimal.valueOf(100))
                .code("FEE0101")
                .version("1")
                .volume(1)
                .build()))
            .build();
        ServiceRequestPaymentDto paymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100))
            .organisationName("TestOrg")
            .currency("GBP")
            .customerReference("123456")
            .idempotencyKey(randomUUID.toString())
            .build();
        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createServiceRequest(serviceRequestDto)
            .then().gotCreated(Map.class, mapResult -> {
                Object serviceRequestReference = mapResult.get("service_request_reference");
                assertThat(serviceRequestReference).isNotNull();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createServiceRequestCreditAccountPayment(paymentDto, serviceRequestReference.toString())
                    .then().gotCreated(ServiceRequestPaymentBo.class, paymentBo -> {
                        paymentReference = paymentBo.getPaymentReference();
                        assertThat(paymentBo.getPaymentReference()).isNotNull();
                        assertThat(paymentBo.getStatus()).isEqualToIgnoringCase("success");
                    });
            });
    }

    private CasePaymentRequest getCasePaymentRequest() {
        CasePaymentRequest casePaymentRequest = CasePaymentRequest.casePaymentRequestWith()
            .action("action")
            .responsibleParty("party")
            .build();

        return casePaymentRequest;
    }

    @After
    public void deletePayment() {
        if (paymentReference != null) {
            // delete payment record
            paymentTestService.deletePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference).then().statusCode(NO_CONTENT.value());
        }
    }

    @AfterClass
    public static void tearDown() {
        if (!userEmails.isEmpty()) {
            // delete idam test user
            userEmails.forEach(IdamService::deleteUser);
        }
    }
}
