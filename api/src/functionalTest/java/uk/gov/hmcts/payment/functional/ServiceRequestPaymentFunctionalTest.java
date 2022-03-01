package uk.gov.hmcts.payment.functional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.CasePaymentRequest;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
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
    private LaunchDarklyFeature featureToggler;

    private static String USER_TOKEN;

    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void createAnServiceRequestAndMakePBAPayment(){
        UUID randomUUID = UUID.randomUUID();
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .hmctsOrgId("ABA1")
            .ccdCaseNumber("1234567890123456")
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
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createServiceRequest(serviceRequestDto)
            .then().gotCreated(Map.class,mapResult->{
            Object serviceRequestReference=mapResult.get("service_request_reference");
            assertThat(serviceRequestReference).isNotNull();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().createServiceRequestCreditAccountPayment(paymentDto,serviceRequestReference.toString())
                .then().gotCreated(ServiceRequestPaymentBo.class, paymentBo->{
                assertThat(paymentBo.getPaymentReference()).isNotNull();
                assertThat(paymentBo.getStatus()).isEqualToIgnoringCase("success");
            });
        });
    }

    private CasePaymentRequest getCasePaymentRequest(){
        CasePaymentRequest casePaymentRequest = CasePaymentRequest.casePaymentRequestWith()
            .action("action")
            .responsibleParty("party")
            .build();

        return casePaymentRequest;
    }
}
