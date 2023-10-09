package uk.gov.hmcts.payment.functional;


import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.validator.routines.UrlValidator;
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
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class RemissionFunctionalTest {

    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";
    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
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
    private PaymentTestService paymentTestService;
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

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
    public void createUpfrontRemission() {
        String ccdCaseNumber = "11115677" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createUpfrontRemission(getRemissionRequest(ccdCaseNumber))
            .then().gotCreated(RemissionDto.class, remissionDto -> {
                assertThat(remissionDto).isNotNull();
                assertThat(remissionDto.getFee()).isEqualToComparingOnlyGivenFields(getFee(ccdCaseNumber));
            });
    }

    @Test
    public void createRetrospectiveRemissionAndRetrieveRemissionByPaymentGroupTest() throws Exception {
        String ccdCaseNumber = "11115652" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("99.99"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("LegacySearch")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        // TEST create telephony card payment8
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

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
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
                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().getRemissions(paymentGroupReference)
                    .then().got(PaymentGroupDto.class, paymentGroupDto -> {
                        assertThat(paymentGroupDto).isNotNull();
                        assertThat(paymentGroupDto.getPayments().get(0)).isEqualToComparingOnlyGivenFields(telephonyPaymentRequest);
                        assertThat(paymentGroupDto.getRemissions().get(0)).isEqualToComparingOnlyGivenFields(getRemissionRequest(ccdCaseNumber));
                        assertThat(paymentGroupDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(getFee(ccdCaseNumber));

                        BigDecimal netAmount = paymentGroupDto.getFees().get(0).getCalculatedAmount()
                            .subtract(paymentGroupDto.getRemissions().get(0).getHwfAmount());
                        assertThat(netAmount).isEqualTo(paymentGroupDto.getFees().get(0).getNetAmount());
                    });

            });
    }


    private RemissionRequest getRemissionRequest(String ccdCaseNumber) {
        return RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber(ccdCaseNumber)
            .hwfAmount(new BigDecimal("50"))
            .hwfReference("HR1111")
            .caseType("DIVORCE_ExceptionRecord")
            .fee(getFee(ccdCaseNumber))
            .build();
    }

    private FeeDto getFee(String ccdCaseNumber) {
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("1")
            .code("FEE0123")
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
