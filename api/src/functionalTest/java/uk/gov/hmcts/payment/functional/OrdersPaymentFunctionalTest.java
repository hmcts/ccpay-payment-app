package uk.gov.hmcts.payment.functional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.dto.CasePaymentRequest;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
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
public class OrdersPaymentFunctionalTest {

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
    public void createAnOrderAndMakePBAPayment(){
        UUID randomUUID = UUID.randomUUID();
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .hmctsOrgId("Divorce")
            .ccdCaseNumber("1234567890123456")
            .caseReference("abcd-defg-hjik-1234")
            .casePaymentRequest(getCasePaymentRequest())
            .fees(Arrays.asList(ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(BigDecimal.valueOf(100))
                .code("FEE0101")
                .version("1")
                .volume(1)
                .build()))
            .build();
        OrderPaymentDto paymentDto = OrderPaymentDto.paymentDtoWith()
            .accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100))
            .currency("GBP")
            .customerReference("123456")
            .build();
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createServiceRequest(serviceRequestDto)
            .then().gotCreated(Map.class,mapResult->{
            Object serviceRequestReference=mapResult.get("service_request_reference");
            assertThat(serviceRequestReference).isNotNull();
            dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().createOrderCreditAccountPayment(paymentDto,serviceRequestReference.toString(),randomUUID.toString())
                .then().gotCreated(OrderPaymentBo.class, paymentBo->{
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
