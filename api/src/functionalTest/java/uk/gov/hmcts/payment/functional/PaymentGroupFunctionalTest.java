package uk.gov.hmcts.payment.functional;


import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.TelephonyCardPaymentsRequest;
import uk.gov.hmcts.payment.api.contract.TelephonyCardPaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.BulkScanPaymentRequest;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PaymentGroupFunctionalTest {
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";
    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
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

    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith("payments");
            USER_TOKEN = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void createUpfrontRemission() {
        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createUpfrontRemission(getRemissionRequest(ccdCaseNumber))
            .then().gotCreated(RemissionDto.class, remissionDto -> {
                assertThat(remissionDto).isNotNull();
                assertThat(remissionDto.getFee()).isEqualToComparingOnlyGivenFields(getFee(ccdCaseNumber));
            });
    }

    @Test
    public void createNewFeeWithPaymentGroup() {

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(getPaymentFeeGroupRequest())
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(getPaymentFeeGroupRequest());
            });
    }

    @Test
    public void givenAFeeAndRemissionInPG_WhenAFeeNeedUpdatingthenFeeShouldBeAddedToExistingGroup() throws Exception {
        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        TelephonyCardPaymentsRequest telephonyCardPaymentsRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("FinancialRemedyContested")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("250.00"))
                .code("FEE3232")
                .version("1")
                .reference("testRef")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        // TEST create telephony card payment
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

                //Test add new Fee to payment group
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().addNewFeetoExistingPaymentGroup(groupDto, paymentGroupReference)
                    .then().got(PaymentGroupDto.class, paymentGroupFeeDto2 -> {
                        assertThat(paymentGroupFeeDto2).isNotNull();
                        assertThat(paymentGroupFeeDto2.getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                    });

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyCardPaymentsRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        assertTrue(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX));
                        assertEquals("payment status is properly set", "Initiated", telephonyCardPaymentsResponse.getStatus());
                        String[] schemes = {"https"};
                        UrlValidator urlValidator = new UrlValidator(schemes);
                        assertNotNull(telephonyCardPaymentsResponse.getLinks().getNextUrl());
                        assertTrue(urlValidator.isValid(telephonyCardPaymentsResponse.getLinks().getNextUrl().getHref()));
                    });

                // TEST create retrospective remission
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createRetrospectiveRemission(getRemissionRequest(ccdCaseNumber), paymentGroupReference, feeId)
                    .then().gotCreated(RemissionDto.class, remissionDto -> {
                        assertThat(remissionDto).isNotNull();
                        assertThat(remissionDto.getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                        assertThat(remissionDto.getRemissionReference().matches(REMISSION_REFERENCE_REGEX)).isTrue();
                    });

                // TEST retrieve payments, remissions and fees by payment-group-reference
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().getRemissions(paymentGroupReference)
                    .then().got(PaymentGroupDto.class, paymentGroupDto -> {
                        assertThat(paymentGroupDto).isNotNull();
                        assertThat(paymentGroupDto.getPayments().get(0)).isEqualToComparingOnlyGivenFields(getCardPaymentRequest(ccdCaseNumber));
                        assertThat(paymentGroupDto.getRemissions().get(0)).isEqualToComparingOnlyGivenFields(getRemissionRequest(ccdCaseNumber));
                        assertThat(paymentGroupDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(getFee(ccdCaseNumber));
                        assertThat(paymentGroupDto.getFees().size()).isEqualTo(2);
                        assertThat(paymentGroupDto.getFees().get(1)).isEqualToComparingOnlyGivenFields(getPaymentFeeGroupRequest());

                        if (paymentGroupDto.getFees().get(0).getCode().equalsIgnoreCase("FEE0123")) {
                            BigDecimal netAmount = paymentGroupDto.getFees().get(0).getCalculatedAmount()
                                .subtract(paymentGroupDto.getRemissions().get(0).getHwfAmount());
                            assertThat(netAmount).isEqualTo(paymentGroupDto.getFees().get(0).getNetAmount());
                        }
                    });

            });
    }


    @Test
    public void givenAFeeInPG_WhenABulkScanPaymentNeedsMappingthenPaymentShouldBeAddedToExistingGroup() throws Exception {
        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal(100.00))
            .service("Divorce")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber("DCN2903423425343478348")
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("450.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
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
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        paymentReference = paymentDto.getReference();
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());
                    });

            });

    }

    @Test
    public void makeAndRetrieveBulkScanPayment_TestShouldReturnAutoApportionedFees() {
        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(20))
            .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal(120.00))
            .service("Divorce")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber("DCN2903423425343478348")
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGrpDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(fees)
            .build();

        AtomicReference<String> paymentRef = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGrpDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGrpDto);


                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        paymentReference = paymentDto.getReference();
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());
                        paymentRef.set(paymentDto.getReference());
                    });
            });

        // TEST retrieve payments, remissions and fees by payment-group-reference
        dsl.given().userToken(USER_TOKEN)
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
    public void givenABulkScanPaymentNeedsMappingthenPaymentShouldBeCreatedWithPaymentGroup() throws Exception {
        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal(100.00))
            .service("Digital Bar")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber("DCN293842342342834278348")
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716276")
            .build();


        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createBulkScanPaymentWithPaymentGroup(bulkScanPaymentRequest)
            .then().gotCreated(PaymentDto.class, paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertThat(paymentDto.getReference()).isNotNull();
                assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().getRemissions(paymentDto.getPaymentGroupReference())
                    .then().got(PaymentGroupDto.class, paymentGroupDto -> {
                        assertThat(paymentGroupDto).isNotNull();
                        assertThat(paymentGroupDto.getPayments().get(0)).isEqualToComparingOnlyGivenFields(bulkScanPaymentRequest);
                        assertThat(paymentGroupDto.getFees().size()).isEqualTo(0);

                    });

            });

    }

    @Test
    public void givenMultipleFeesAndRemissionWithPaymentInPG_WhenCaseIsSearchedShouldBeReturned() throws Exception {

        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        RemissionRequest remissionRequest = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber(ccdCaseNumber)
            .hwfAmount(new BigDecimal("50"))
            .hwfReference("HR1111")
            .caseType("MoneyClaimCase")
            .fee(getFee(ccdCaseNumber))
            .build();


        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(getPaymentFeeGroupRequest())
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(getPaymentFeeGroupRequest());

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();
                FeeDto feeDto1 = paymentGroupFeeDto.getFees().get(0);
                Integer feeId = feeDto1.getId();

                // TEST create retrospective remission
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createRetrospectiveRemission(remissionRequest, paymentGroupReference, feeId)
                    .then().gotCreated(RemissionDto.class, remissionDto -> {
                        assertThat(remissionDto).isNotNull();
                        assertThat(remissionDto.getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                        assertThat(remissionDto.getRemissionReference().matches(REMISSION_REFERENCE_REGEX)).isTrue();
                    });

                // TEST retrieve payments, remissions and fees by payment-group-reference
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().getPaymentGroups(ccdCaseNumber)
                    .then().getPaymentGroups((paymentGroupsResponse -> {
                        Assertions.assertThat(paymentGroupsResponse.getPaymentGroups().size()).isEqualTo(1);
                    }));

            });
    }

    @Test
    public void givenMultipleFeesAndRemissionWithPaymentInPG_WhenCaseIsSearchedShouldBeReturnedForFinrem() throws Exception {

        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("1")
            .code("FEE0123")
            .build();

        RemissionRequest remissionRequest = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber(ccdCaseNumber)
            .hwfAmount(new BigDecimal("50"))
            .hwfReference("HR1111")
            .caseType("MoneyClaimCase")
            .fee(getFee(ccdCaseNumber))
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("FinancialRemedyContested")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("250.00"))
                .code("FEE3232")
                .version("1")
                .reference("testRef")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        // create group
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(getPaymentFeeGroupRequest())
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {

                // TEST create telephony card payment
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(feeDto);
                        assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();

                        String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();
                        FeeDto feeDto1 = paymentGroupFeeDto.getFees().get(0);
                        Integer feeId = feeDto1.getId();

                        // TEST create retrospective remission
                        dsl.given().userToken(USER_TOKEN)
                            .s2sToken(SERVICE_TOKEN)
                            .when().createRetrospectiveRemission(remissionRequest, paymentGroupReference, feeId)
                            .then().gotCreated(RemissionDto.class, remissionDto -> {
                                assertThat(remissionDto).isNotNull();
                                assertThat(remissionDto.getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                                assertThat(remissionDto.getRemissionReference().matches(REMISSION_REFERENCE_REGEX)).isTrue();
                            });

                        //Test add new Fee to payment group
                        dsl.given().userToken(USER_TOKEN)
                            .s2sToken(SERVICE_TOKEN)
                            .when().addNewFeetoExistingPaymentGroup(groupDto, paymentGroupReference)
                            .then().got(PaymentGroupDto.class, paymentGroupFeeDto2 -> {
                                assertThat(paymentGroupFeeDto2).isNotNull();
                                assertThat(paymentGroupFeeDto2.getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                            });

                        // TEST retrieve payments, remissions and fees by payment-group-reference
                        dsl.given().userToken(USER_TOKEN)
                            .s2sToken(SERVICE_TOKEN)
                            .when().getPaymentGroups(ccdCaseNumber)
                            .then().getPaymentGroups((paymentGroupsRes -> {
                                Assertions.assertThat(paymentGroupsRes.getPaymentGroups().size()).isEqualTo(1);
                            }));

                    });
            });
    }

    @Test
    public void givenFeesWithPaymentInPG_WhenCaseIsSearchedShouldBeReturned() throws Exception {

        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("110.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("1")
            .code("FEE0123")
            .description("Application for a third party debt order")
            .jurisdiction1("civil")
            .jurisdiction2("Country")
            .memoLine("Receipt of Fees")
            .naturalAccountCode("4481102145")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("110"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

/*
        TelephonyPaymentRequest telephonyPaymentRequest = TelephonyPaymentRequest.createTelephonyPaymentRequestDtoWith()
            .amount(new BigDecimal("110"))
            .ccdCaseNumber(ccdCaseNumber)
            .channel("telephony")
            .currency(CurrencyCode.GBP)
            .description("A test telephony payment")
            .provider("pci pal")
            .caseType("DIVORCE")
            .build();
*/

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");

                    });

            });

    }

    @Test
    public void givenFeesWithPaymentInPG_WhenCaseIsSearchedShouldBeReturnedForPCIPALAntennaChanges() throws Exception {
        String ccdCaseNumber = "11112222" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("110.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("1")
            .code("FEE0123")
            .description("Application for a third party debt order")
            .jurisdiction1("civil")
            .jurisdiction2("Country")
            .memoLine("Receipt of Fees")
            .naturalAccountCode("4481102145")
            .build();

        TelephonyCardPaymentsRequest telephonyCardPaymentsRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("110"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://google.co.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://google.co.uk")
                    .when().createTelephonyPayment(telephonyCardPaymentsRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");

                    });
            });

    }

    private CardPaymentRequest getCardPaymentRequest(String ccdCaseNumber) {
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .channel("telephony")
            .currency(CurrencyCode.GBP)
            .description("A test telephony payment")
            .provider("pci pal")
            .service("DIVORCE")
            .siteId("AA001")
            .fees(Collections.singletonList(getFee(ccdCaseNumber)))
            .build();
    }

    private RemissionRequest getRemissionRequest(String ccdCaseNumber) {
        return RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber(ccdCaseNumber)
            .hwfAmount(new BigDecimal("50"))
            .hwfReference("HR1111")
            .caseType("MoneyClaimCase")
            .fee(getFee(ccdCaseNumber))
            .build();
    }

    private PaymentGroupDto getPaymentFeeGroupRequest() {
        return PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("250.00"))
                .code("FEE3232")
                .version("1")
                .reference("testRef")
                .volume(2)
                .build())).build();
    }

    private FeeDto getFee(String ccdCaseNumber) {
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("1")
            .code("FEE0123")
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
