package uk.gov.hmcts.payment.functional;

import com.mifmif.common.regex.Generex;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PciPalCallbackTest {
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
    @Autowired
    private PaymentTestService paymentTestService;
    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user1 = idamService.createUserWith("citizen");
            USER_TOKEN = user1.getAuthorisationToken();
            userEmails.add(user1.getEmail());
            User user2 = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user2.getAuthorisationToken();
            userEmails.add(user2.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void updateTelephonyPayment_shouldReturnSucceess() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);

        // create telephony payment using old api
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().getByStatusCode(201);

        assertNotNull(paymentDto.getReference());

        paymentReference = paymentDto.getReference();

        //pci-pal callback
        TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
            .orderReference(paymentReference)
            .orderAmount("9999")
            .transactionResult("SUCCESS")
            .build();

        dsl.given().s2sToken(SERVICE_TOKEN)
            .when().telephonyCallback(callbackDto)
            .then().noContent();

        // retrieve payment
        paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(paymentReference)
            .then().get();

        assertNotNull(paymentDto);
        assertEquals(paymentDto.getReference(), paymentReference);
        assertEquals(paymentDto.getExternalProvider(), "pci pal");
        assertEquals(paymentDto.getStatus(), "Success");
    }

    @Test
    public void makeAndRetrievePCIPALPayment_Success_TestShouldReturnAutoApportionedFees() {
        final String[] reference = new String[1];
        // create card payment

        String ccdCaseNumber = "198765432101" + String.format("%04d", new Random().nextInt(10000));

        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(20))
            .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());


        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("120"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .telephonySystem("Kerv")
            .returnURL("https://google.co.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(fees).build();

        AtomicReference<String> paymentRef = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                Assertions.assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        Assertions.assertThat(telephonyCardPaymentsResponse).isNotNull();
                        Assertions.assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        Assertions.assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentRef.set(telephonyCardPaymentsResponse.getPaymentReference());
                    });

            });

        //pci-pal callback
        TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
            .orderReference(paymentRef.get())
            .orderAmount("120")
            .transactionResult("SUCCESS")
            .build();

        dsl.given().s2sToken(SERVICE_TOKEN)
            .when().telephonyCallback(callbackDto)
            .then().noContent();

        // retrieve payment
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(paymentRef.get())
            .then().get();

        assertNotNull(paymentDto);
        assertEquals(paymentDto.getReference(), paymentRef.get());
        assertEquals(paymentDto.getExternalProvider(), "pci pal");
        assertEquals(paymentDto.getStatus(), "Success");

        // TEST retrieve payments, remissions and fees by payment-group-reference
        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().getPaymentGroups(ccdCaseNumber)
            .then().getPaymentGroups((paymentGroupsResponse -> {
                paymentGroupsResponse.getPaymentGroups().stream()
                    .filter(paymentGroupDto -> paymentGroupDto.getPayments().get(0).getReference()
                        .equalsIgnoreCase(paymentRef.get()))
                    .forEach(paymentGroupDto -> {

                        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature", false);
                        if (apportionFeature) {
                            paymentGroupDto.getFees().stream()
                                .filter(fee -> fee.getCode().equalsIgnoreCase("FEE0271"))
                                .forEach(fee -> {
                                    assertEquals(BigDecimal.valueOf(0).intValue(), fee.getAmountDue().intValue());
                                });
                            paymentGroupDto.getFees().stream()
                                .filter(fee -> fee.getCode().equalsIgnoreCase("FEE0272"))
                                .forEach(fee -> {
                                    assertEquals(BigDecimal.valueOf(0).intValue(), fee.getAmountDue().intValue());
                                });
                            paymentGroupDto.getFees().stream()
                                .filter(fee -> fee.getCode().equalsIgnoreCase("FEE0273"))
                                .forEach(fee -> {
                                    assertEquals(BigDecimal.valueOf(0).intValue(), fee.getAmountDue().intValue());
                                });
                        }
                    });
            }));
    }

    @Test
    public void makeAndRetrievePCIPALPayment_Failed_TestShouldReApportionFees() {
        final String[] reference = new String[1];
        // create card payment

        String ccdCaseNumber = "198765432101" + String.format("%04d", new Random().nextInt(10000));

        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(20))
            .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());


        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("120"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .telephonySystem("Kerv")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(fees).build();

        AtomicReference<String> paymentRef = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                Assertions.assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        Assertions.assertThat(telephonyCardPaymentsResponse).isNotNull();
                        Assertions.assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        Assertions.assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentRef.set(telephonyCardPaymentsResponse.getPaymentReference());
                    });
            });

        //pci-pal callback
        TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
            .orderReference(paymentRef.get())
            .orderAmount("120")
            .transactionResult("FAILED")
            .build();

        dsl.given().s2sToken(SERVICE_TOKEN)
            .when().telephonyCallback(callbackDto)
            .then().noContent();

        // TEST retrieve payments, remissions and fees by payment-group-reference
        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().getPaymentGroups(ccdCaseNumber)
            .then().getPaymentGroups((paymentGroupsResponse -> {
                //Assertions.assertThat(paymentGroupsResponse.getPaymentGroups().size()).isEqualTo(1);
                paymentGroupsResponse.getPaymentGroups().stream()
                    .filter(paymentGroupDto -> paymentGroupDto.getPayments().get(0).getReference()
                        .equalsIgnoreCase(paymentRef.get()))
                    .forEach(paymentGroupDto -> {

                        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature", false);
                        if (apportionFeature) {
                            paymentGroupDto.getFees().stream()
                                .filter(fee -> fee.getCode().equalsIgnoreCase("FEE0271"))
                                .forEach(fee -> {
                                    assertEquals(BigDecimal.valueOf(20).intValue(), fee.getAmountDue().intValue());
                                });
                            paymentGroupDto.getFees().stream()
                                .filter(fee -> fee.getCode().equalsIgnoreCase("FEE0272"))
                                .forEach(fee -> {
                                    assertEquals(BigDecimal.valueOf(40).intValue(), fee.getAmountDue().intValue());
                                });
                            paymentGroupDto.getFees().stream()
                                .filter(fee -> fee.getCode().equalsIgnoreCase("FEE0273"))
                                .forEach(fee -> {
                                    assertEquals(BigDecimal.valueOf(60).intValue(), fee.getAmountDue().intValue());
                                });
                        }
                    });
            }));
    }

    @Test
    public void updateTelephonyPayment_shouldReturnNotFoundWhenPaymentReferenceNotFound() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);

        // create telephony payment using old api
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertNotNull(paymentDto.getReference());
            });

        //pci-pal callback
        TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
            .orderReference("RC-Invalid-reference")
            .orderAmount("9999")
            .transactionResult("SUCCESS")
            .build();

        dsl.given().s2sToken(SERVICE_TOKEN)
            .when().telephonyCallback(callbackDto)
            .then().notFound();
    }

    private PaymentRecordRequest getTelephonyPayment(String reference) {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .externalProvider("pci pal")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build())
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference(reference)
            .service("CMC")
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

    @After
    public void deletePayment() {
        if (paymentReference != null) {
            // delete payment record
            paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference).then().statusCode(NO_CONTENT.value());
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
