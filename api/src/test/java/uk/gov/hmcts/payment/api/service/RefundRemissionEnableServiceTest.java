
package uk.gov.hmcts.payment.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.domain.model.Roles;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.util.RefundEligibilityUtil;

@ActiveProfiles({"local", "test"})
@SpringBootTest(webEnvironment = MOCK)
public class RefundRemissionEnableServiceTest {

    public static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";
    public static final String SOME_SERVICE_AUTHORIZATION_TOKEN = "ServiceToken";
    final static MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

    @InjectMocks
    private RefundRemissionEnableServiceImpl refundRemissionEnableServiceImpl;
    @Mock
    private RefundEligibilityUtil refundEligibilityUtil;
    @Mock
    private LaunchDarklyFeatureToggler featureToggler;
    @Mock
    private IdamService idamService;
    @Autowired
    private Roles roles;
    @Mock
    private FeePayApportionRepository feePayApportionRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private RemissionRepository remissionRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        header.put("Authorization", Collections.singletonList(SOME_AUTHORIZATION_TOKEN));
        header.put("ServiceAuthorization",
            Collections.singletonList(SOME_SERVICE_AUTHORIZATION_TOKEN));
        header.put("content-type", Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
    }

    @After
    public void tearDown() {
        header.clear();
    }

    private static final IdamUserIdResponse IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE =
        IdamUserIdResponse.idamUserIdResponseWith().uid("1").givenName("XX").familyName("YY")
            .name("XX YY")
            .roles(Arrays.asList("payments-refund-approver", "payments-refund")).sub("ZZ")
            .build();
    private static final IdamUserIdResponse IDAM_USER_ID_RESPONSE_NO_REFUND_ROLE =
        IdamUserIdResponse.idamUserIdResponseWith().uid("1").givenName("XX").familyName("YY")
            .name("XX YY")
            .roles(Arrays.asList("testRole")).sub("ZZ")
            .build();
    private static final IdamUserIdResponse IDAM_USER_ID_RESPONSE_ONE_REFUND_ROLE =
        IdamUserIdResponse.idamUserIdResponseWith().uid("1").givenName("XX").familyName("YY")
            .name("XX YY")
            .roles(Arrays.asList("payments-refund-approver")).sub("ZZ")
            .build();

    DateUtil date = new DateUtil();
    Date paymentUpdateDate = date.getIsoDateTimeFormatter().parseDateTime("2021-11-02T21:48:07")
        .toDate();

    PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
        .id(1)
        .paymentReference("2018-15202505035")
        .fees(Arrays.asList(getPaymentFeeWithOutRemission()))
        .build();

    FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
        .apportionAmount(new BigDecimal("99.99"))
        .paymentAmount(new BigDecimal("99.99"))
        .ccdCaseNumber("1234123412341234")
        .paymentLink(paymentFeeLink)
        .paymentId(1)
        .feeId(1)
        .id(1)
        .feeAmount(new BigDecimal("99.99")).build();

    @Test
    public void returnTrueWhenAllRolesAndPaymentSucessfullWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getSuccessPayment());
        Assert.assertEquals(isRefundEnable, true);
    }

    @Test
    public void returnTrueWhenOneRolesAndPaymentSucessfullWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ONE_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getSuccessPayment());
        Assert.assertEquals(isRefundEnable, true);
    }

    @Test
    public void returnFalseWhenRolesNotPresentAndPaymentSucessfullWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_NO_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getSuccessPayment());
        Assert.assertEquals(isRefundEnable, false);
    }

    @Test
    public void returnFalseWhenRolesNotPresentAndPaymentIsFailedlWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_NO_REFUND_ROLE);

        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getFailedPayment());
        Assert.assertEquals(isRefundEnable, false);
    }

    @Test
    public void returnFalseWhenARolesPresentAndPaymentIsFailedlWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);

        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getFailedPayment());
        Assert.assertEquals(isRefundEnable, false);
    }

    @Test
    public void returnTrueWhenRolesPresentAndPaymentIsSucessfullAndLagTimeEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getSuccessPayment());
        Assert.assertEquals(isRefundEnable, true);

    }

    @Test
    public void returnFalseWhenRolesNotPresentAndPaymentIsSucessfullAndLagTimeEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_NO_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getSuccessPayment());
        Assert.assertEquals(isRefundEnable, false);

    }

    @Test
    public void returnFalseWhenRolesPresentAndPaymentIsFailedAndLagTimeEligibleTrueWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getFailedPayment());
        Assert.assertEquals(isRefundEnable, false);

    }

    @Test
    public void returnFalseWhenRolesPresentAndPaymentIsSucessButLagEligibleIsFalseWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(false);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getSuccessPayment());
        Assert.assertEquals(isRefundEnable, false);
    }
    @Test
    public void returnTrueWhenOneRolePresentAndPaymentIsSucessfullAndLagTimeEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ONE_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRefundEnable = refundRemissionEnableServiceImpl.returnRefundEligible(
            getSuccessPayment());
        Assert.assertEquals(isRefundEnable, true);
    }
    @Test
    public void returnFalseWhenRolesPresentAndPRemissionPresentWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(remission));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithRemission());
        Assert.assertEquals(isRemissionEnable, false);
    }

    @Test
    public void returnTrueWhenRolesPresentAndPRemissionNotPresentWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(null));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithOutRemission());
        Assert.assertEquals(isRemissionEnable, true);
    }

    @Test
    public void returnFalseWhenNoRolesPresentAndPRemissionNotPresentWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_NO_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(null));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithOutRemission());
        Assert.assertEquals(isRemissionEnable, false);

    }

    @Test
    public void returnTrueWhenOneRolePresentAndPRemissionNotPresentWhenFeatureToggleDisable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ONE_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(false);
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(null));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithOutRemission());
        Assert.assertEquals(isRemissionEnable, true);
    }

    @Test
    public void returnTrueWhenRolesPresentAndPRemissionPresentAndLagTimeEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(feePayApportionRepository.findByFeeId(any())).thenReturn(
            Optional.ofNullable(feePayApportion));
        when(paymentService.getPaymentById(any())).thenReturn(getSuccessPayment());
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(null));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithOutRemission());
        // Assert.assertEquals(isRemissionEnable, true);
    }
    @Test
    public void returnFalseWhenRolesPresentAndPRemissionNotPresentAndLagTimeNotEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ALL_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(feePayApportionRepository.findByFeeId(any())).thenReturn(
            Optional.ofNullable(feePayApportion));
        when(paymentService.getPaymentById(any())).thenReturn(getSuccessPayment());
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(false);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(null));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithOutRemission());
        Assert.assertEquals(isRemissionEnable, false);
    }

    @Test
    public void returnTrueWhenOneRolePresentAndPRemissionNotPresentAndLagTimeEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_ONE_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(feePayApportionRepository.findByFeeId(any())).thenReturn(
            Optional.ofNullable(feePayApportion));
        when(paymentService.getPaymentById(any())).thenReturn(getSuccessPayment());
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(null));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithOutRemission());
        //Assert.assertEquals(isRemissionEnable, true);
    }

    @Test
    public void returnFalseWhenNoRolePresentAndPRemissionNotPresentAndLagTimeEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_NO_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(feePayApportionRepository.findByFeeId(any())).thenReturn(
            Optional.ofNullable(feePayApportion));
        when(paymentService.getPaymentById(any())).thenReturn(getSuccessPayment());
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(null));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithOutRemission());
        Assert.assertEquals(isRemissionEnable, false);
    }

    @Test
    public void returnFalseWhenRolesPresentAndPRemissionPresentAndLagTimeEligibleWhenFeatureToggleEnable() {

        when(idamService.getUserId(any())).thenReturn(IDAM_USER_ID_RESPONSE_NO_REFUND_ROLE);
        when(featureToggler.getBooleanValue("refund-remission-lagtime-feature", false)).thenReturn(true);
        when(feePayApportionRepository.findByFeeId(any())).thenReturn(
            Optional.ofNullable(feePayApportion));
        when(paymentService.getPaymentById(any())).thenReturn(getSuccessPayment());
        when(refundEligibilityUtil.getRefundEligiblityStatus(any(Payment.class),
            any(Long.class))).thenReturn(true);
        when(remissionRepository.findByFeeId(getPaymentFeeWithRemission().getId())).thenReturn(
            Optional.ofNullable(remission));
        boolean IsRole=refundRemissionEnableServiceImpl.isRolePresent(header);
        Boolean isRemissionEnable = refundRemissionEnableServiceImpl.returnRemissionEligible(
            getPaymentFeeWithRemission());
        Assert.assertEquals(isRemissionEnable, false);

    }

    private PaymentFee getPaymentFeeWithRemission() {
        return PaymentFee.feeWith()
            .calculatedAmount(new BigDecimal("99.99"))
            .version("1").code("FEE0001").volume(1)
            .id(1)
            .paymentLink(PaymentFeeLink.paymentFeeLinkWith()
                .paymentReference("payment-reference")
                .dateCreated(new Date(2021, 1, 1))
                .dateUpdated(new Date(2021, 1, 1))
                .id(1)
                .build())
            .remissions(Arrays.asList(
                Remission.remissionWith()
                    .remissionReference("remission-reference")
                    .beneficiaryName("beneficiary-name")
                    .ccdCaseNumber("ccd_case_number")
                    .caseReference("case_reference")
                    .hwfReference("hwf-reference")
                    .hwfAmount(new BigDecimal("100.00"))
                    .fee(PaymentFee.feeWith().feeAmount(new BigDecimal("100.00")).id(1).build())
                    .dateCreated(new Date(2021, 1, 1))
                    .id(1)
                    .build()
            ))
            .build();
    }

    private PaymentFee getPaymentFeeWithOutRemission() {
        return PaymentFee.feeWith()
            .calculatedAmount(new BigDecimal("99.99"))
            .version("1").code("FEE0001").volume(1)
            .feeAmount(new BigDecimal("99.99"))
            .ccdCaseNumber("ccd_case_number")
            .id(1)
            .build();
    }

    private Payment getSuccessPayment() {
        return Payment.paymentWith()
            .id(1)
            .amount(new BigDecimal("99.99"))
            .caseReference("caseReference")
            .description("retrieve payment mock test")
            .serviceType("Civil Money Claims")
            .siteId("siteID")
            .currency("GBP")
            .organisationName("organisationName")
            .customerReference("customerReference")
            .pbaNumber("pbaNumer")
            .reference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("1234123412341234")
            .dateUpdated(paymentUpdateDate)
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .build();
    }

    private Payment getFailedPayment() {
        return Payment.paymentWith()
            .id(1)
            .amount(new BigDecimal("99.99"))
            .caseReference("caseReference")
            .description("retrieve payment mock test")
            .serviceType("Civil Money Claims")
            .siteId("siteID")
            .currency("GBP")
            .organisationName("organisationName")
            .customerReference("customerReference")
            .pbaNumber("pbaNumer")
            .reference("RC-1520-2505-0381-8145")
            .ccdCaseNumber("1234123412341234")
            .dateUpdated(paymentUpdateDate)
            .paymentStatus(PaymentStatus.paymentStatusWith().name("failed").build())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .build();
    }

    Remission remission = Remission.remissionWith()
        .remissionReference("12345")
        .hwfReference("HR1111")
        .hwfAmount(new BigDecimal("50.00"))
        .ccdCaseNumber("1111-2222-2222-1111")
        .siteId("AA001")
        .id(1)
        .build();
}
