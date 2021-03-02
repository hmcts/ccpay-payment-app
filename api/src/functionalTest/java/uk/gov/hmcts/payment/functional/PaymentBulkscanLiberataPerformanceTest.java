package uk.gov.hmcts.payment.functional;


import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentAllocationDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.model.PaymentAllocationStatus;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PaymentBulkscanLiberataPerformanceTest {

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
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";

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
    public void givenAFeeInPG_WhenABulkScanPaymentNeedsMappingthenPaymentShouldBeAddedToExistingGroup() throws Exception {
//        String ccdCaseNumber = "1111-CC12-" + RandomUtils.nextInt();
        String ccdCaseNumber = "5643213213000700";
        String ccdCaseNumber1 = "1111-CC12-" + RandomUtils.nextInt();
//        String dcn = "3456908723459" + RandomUtils.nextInt();
        String dcn = "700000000000100000699";

        BulkScanPaymentRequestStrategic bulkScanPaymentRequest = BulkScanPaymentRequestStrategic.createBulkScanPaymentStrategicWith()
            .amount(new BigDecimal(100.00))
            .service(Service.DIVORCE)
            .siteId("AA08")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .externalProvider("exela")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .paymentAllocationDTO(getPaymentAllocationDto("Allocated","bulk scan allocation",null))
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("550.00"))
                .code("FEE0002")
                .version("4")
                .reference("testRef1")
                .volume(4)
                .ccdCaseNumber(ccdCaseNumber1)
                .build())).build();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
            assertThat(paymentGroupFeeDto).isNotNull();
            assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
            assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);


            dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().createBulkScanPaymentStrategic(bulkScanPaymentRequest,paymentGroupFeeDto.getPaymentGroupReference())
                .then().gotCreated(PaymentDto.class, paymentDto -> {
                    assertThat(paymentDto.getReference()).isNotNull();
                    assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                    assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());
            });

        });

    }

    private PaymentAllocationDto getPaymentAllocationDto(String paymentAllocationStatus, String paymentAllocationDescription, String unidentifiedReason) {
        return PaymentAllocationDto.paymentAllocationDtoWith()
            .dateCreated(new Date())
            .explanation("test explanation")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith()
                .description(paymentAllocationDescription).name(paymentAllocationStatus).build())
            .reason("testReason")
            .receivingOffice("testReceivingOffice")
            .unidentifiedReason(unidentifiedReason)
            .userId("AA08")
            .userName("testname")
            .build();
    }
}
