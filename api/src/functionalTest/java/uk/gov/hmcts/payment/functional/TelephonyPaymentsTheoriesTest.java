package uk.gov.hmcts.payment.functional;

import com.mifmif.common.regex.Generex;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(Theories.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class TelephonyPaymentsTheoriesTest {
    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static final String DATE_TIME_FORMAT_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";

    @Before
    public void setUp() throws Exception {
        TestContextManager tcm = new TestContextManager(getClass());
        tcm.prepareTestInstance(this);

        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    public static ServiceDataPoint[] serviceValidations() {
        // get information from Jaedan which siteIDs should be tested and fill them out here
        return new ServiceDataPoint[]{
            ServiceDataPoint.of(Service.DIVORCE, "AA01"),
            ServiceDataPoint.of(Service.PROBATE, "AA02"),
            ServiceDataPoint.of(Service.CMC, "AA03")
        };
    }

    // make the payment group - you get the payGroupRef
    // create remission - you get the remissionRef
    // make the payment for the remainder of the remission (set paymentGroupRef in the external_reference in paymentRequestDTO)

    //This should be tested for the following:
    //
    //Full payment of fee
    //Partial payment of fee
    //Against the following services:
    //
    //Divorce
    //Probate
    //CMC
    //As part of this ticket, we need to ensure that the right service site IDs are sent to Liberata for the right services

    @Theory
    public void fullPaymentForFeeForService(ServiceDataPoint dataPoint) {
        // make the payment group - you get the payGroupRef
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(getPaymentFeeGroupRequest())
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
            assertThat(paymentGroupFeeDto).isNotNull();
            assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
            assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(getPaymentFeeGroupRequest());


            String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();
            FeeDto feeDto = paymentGroupFeeDto.getFees().get(0);
            Integer feeId = feeDto.getId();

            // create remission - you get the remissionRef
            dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().createRetrospectiveRemission(getRemissionRequest(), paymentGroupReference, feeId)
                .then().gotCreated(RemissionDto.class, remissionDto -> {
                assertThat(remissionDto).isNotNull();
                assertThat(remissionDto.getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                assertThat(remissionDto.getRemissionReference().matches(REMISSION_REFERENCE_REGEX)).isTrue();

                // make the payment for the remainder of the remission (set paymentGroupRef in the external_reference in paymentRequestDTO)
                String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
                PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);
                paymentRecordRequest.setExternalReference(paymentGroupReference);
                paymentRecordRequest.setService(dataPoint.service);
                paymentRecordRequest.setSiteId(dataPoint.siteId);
                String status = "success";

                String startDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.goooooogle.com")
                    .when().createTelephonyPayment(paymentRecordRequest)
                    .then().created(paymentDto -> {
                    String referenceNumber = paymentDto.getReference();
                    assertEquals("payment status is properly set", "Success", paymentDto.getStatus());
                    //update the status
                    dsl.given().userToken(USER_TOKEN)
                        .s2sToken(SERVICE_TOKEN)
                        .returnUrl("https://www.goooooogle.com")
                        .when().updatePaymentStatus(referenceNumber, status)
                        .then().noContent();

                    String endDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

                    dsl.given().userToken(USER_TOKEN)
                        .s2sToken(SERVICE_TOKEN)
                        .returnUrl("https://www.goooooogle.com")
                        .when()
                        .enableSearch()
                        .searchPaymentsByServiceBetweenDates(paymentRecordRequest.getService(), startDateTime, endDateTime)
                        .then().got(PaymentsResponse.class, paymentsResponse -> {
                        assertTrue("correct payment has been retrieved",
                            paymentsResponse.getPayments().stream()
                                .anyMatch(o -> o.getPaymentReference().equals(referenceNumber)));
                        PaymentDto paymentRetrieved = paymentsResponse.getPayments().stream().filter(o -> o.getPaymentReference().equals(referenceNumber)).findFirst().get();
                        assertEquals("correct payment reference retrieved", paymentRetrieved.getCaseReference(), paymentRecordRequest.getReference());
                        assertEquals("correct payment group reference retrieved", paymentRetrieved.getExternalReference(), paymentRecordRequest.getExternalReference());
                        assertEquals("payment status is properly set", "Success", paymentRetrieved.getStatus());
                        assertEquals("correct site ID has been retrieved", dataPoint.service.getName(), paymentRetrieved.getServiceName());
                        assertEquals("correct service has been retrieved", dataPoint.siteId, paymentRetrieved.getSiteId());
                    });
                });
            });
        });
    }


    @Theory
    public void partialPaymentForFeeForService(ServiceDataPoint dataPoint) {
        // TODO partial payment
    }

    private PaymentGroupDto getPaymentFeeGroupRequest() {
        return PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("250.00"))
                .code("FEE3232")
                .version("1")
                .reference("testRef")
                .volume(2)
                .ccdCaseNumber("1111-CCD2-3333-4444")
                .build())).build();
    }

    private PaymentRecordRequest getTelephonyPayment(String reference) {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .externalProvider("pci pal")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build())
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference(reference)
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .externalReference(reference)
            .siteId("AA01")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE012345")
                        .reference("ref_1234")
                        .version("1")
                        .volume(1)
                        .build()
                )
            )
            .reportedDateOffline(DateTime.now().toString())
            .build();
    }

    private PaymentRecordRequest getTelephonyPartialPayment(String reference) {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .externalProvider("pci pal")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build())
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference(reference)
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .externalReference(reference)
            .siteId("AA01")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE012345")
                        .reference("ref_1234")
                        .version("1")
                        .volume(1)
                        .build()
                )
            )
            .reportedDateOffline(DateTime.now().toString())
            .build();
    }

    private RemissionRequest getRemissionRequest() {
        return RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber("1111-2222-3333-4444")
            .hwfAmount(new BigDecimal("50"))
            .hwfReference("HR1111")
            .siteId("Y431")
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

    @Data(staticConstructor = "of")
    static class ServiceDataPoint {
        private final Service service;
        private final String siteId;
    }
}
