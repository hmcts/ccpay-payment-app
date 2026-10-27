package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentAllocationDto;
import uk.gov.hmcts.payment.api.contract.TelephonyCardPaymentsRequest;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.BulkScanPaymentRequest;
import uk.gov.hmcts.payment.api.dto.BulkScanPaymentRequestStrategic;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.dto.ResubmitRefundRemissionRequest;
import uk.gov.hmcts.payment.api.dto.RetroRemissionRequest;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.external.client.dto.TelephonyProviderAuthorisationResponse;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentAllocationStatus;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.reports.FeesRegisterClient;
import uk.gov.hmcts.payment.api.service.IdamService;
import uk.gov.hmcts.payment.api.service.KervTelephonySystem;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.service.TelephonySystem;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * These tests exercise the payment-group and remission endpoints with real services, mappers and
 * PostgreSQL. External services are mocked. Each request uses the application's own transactions.
 * The database assertions run after the request completes to verify that its changes were committed.
 *
 * The committed test data is not rolled back automatically. Sharing a database with other test
 * classes allowed those records to appear in their queries and made results depend on test order.
 * The distinct Testcontainers JDBC URL gives this class its own PostgreSQL container and database,
 * so it can verify committed changes without affecting other component test classes.
 *
 * @see ServiceRequestComponentTest
 *
 * @author jhilling
 *
 */
@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK,
    properties = {
        "bulk.scanning.payments.processed.url=http://bulk-scan.test",
        "spring.datasource.url=jdbc:tc:postgresql:11.0://localhost/payment_group_component_tests"
    })
public class PaymentGroupComponentTest {

    private static final String USER_ID = UserResolverBackdoor.CASEWORKER_ID;
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("10.00");
    private static final String SERVICE_NAME = "Divorce";
    private static final String SITE_ID = "AA01";
    private static final String BULK_SCAN_STATUS_URL =
        "http://bulk-scan.test/bulk-scan-payments/{dcn}/status/{status}";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private PaymentFeeRepository paymentFeeRepository;

    @Autowired
    private Payment2Repository paymentRepository;

    @Autowired
    private RemissionRepository remissionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReferenceDataService<SiteDTO> referenceDataService;

    @MockitoBean
    private FeesRegisterClient feesRegisterClient;

    @MockitoBean
    private RefundRemissionEnableService refundRemissionEnableService;

    @MockitoBean
    private LaunchDarklyFeatureToggler featureToggler;

    @MockitoBean
    private PciPalPaymentService pciPalPaymentService;

    @MockitoBean
    private IdamService idamService;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean(name = "restTemplatePaymentGroup")
    private RestTemplate restTemplatePaymentGroup;

    private RestActions restActions;

    @Before
    public void setup() {
        restActions = new RestActions(
            webAppContextSetup(webApplicationContext).apply(springSecurity()).build(),
            serviceRequestAuthorizer, userRequestAuthorizer, objectMapper)
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID);

        when(referenceDataService.getOrganisationalDetail(any(), any(), any()))
            .thenReturn(OrganisationalServiceDto.orgServiceDtoWith()
                .serviceCode(SITE_ID).serviceDescription(SERVICE_NAME).build());
        when(referenceDataService.getSiteIDs()).thenReturn(List.of(SiteDTO.siteDTOwith().siteID(SITE_ID).build()));
        // Mocked flags default to false, keeping apportionment outside these endpoint tests.
    }

    /**
     * Test that a service request that has not been cancelled can receive another fee.
     * The response contains both fees, and the database retains the original fee alongside the new one.
     */
    @Test
    public void addingFeeToOpenServiceRequestPersistsItAlongsideTheOriginalFee() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        Integer originalFeeId = serviceRequest.getFees().getFirst().getId();
        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees(List.of(feeRequest("FEE0002", serviceRequest.getCcdCaseNumber())))
            .build();

        restActions.put("/payment-groups/%s".formatted(serviceRequest.getPaymentReference()), request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payment_group_reference").value(serviceRequest.getPaymentReference()))
            .andExpect(jsonPath("$.fees", hasSize(2)));

        List<PaymentFee> fees = paymentFeeRepository.findByPaymentLinkId(serviceRequest.getId()).orElseThrow();
        assertThat(fees).hasSize(2);
        assertThat(fees).extracting(PaymentFee::getCode).containsExactlyInAnyOrder("FEE0001", "FEE0002");
        assertThat(fees).extracting(PaymentFee::getId).contains(originalFeeId);
        assertThat(fees).allSatisfy(fee -> assertThat(fee.getCalculatedAmount()).isEqualByComparingTo(FEE_AMOUNT));
        assertNotCancelled(serviceRequest);
    }

    /**
     * Test that a bulk-scan payment can be added to a service request that has not been cancelled.
     * The response identifies the payment, which is saved against the existing service request.
     */
    @Test
    public void bulkScanPaymentForOpenServiceRequestIsPersisted() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        String dcn = UUID.randomUUID().toString();
        BulkScanPaymentRequest request = bulkScanRequest(serviceRequest, dcn);

        String response = restActions.post("%s/bulk-scan-payments".formatted(paymentGroupPath(serviceRequest)), request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Success"))
            .andExpect(jsonPath("$.payment_group_reference").value(serviceRequest.getPaymentReference()))
            .andReturn().getResponse().getContentAsString();

        Payment payment = persistedPayment(response, "reference", serviceRequest, "success");
        assertThat(payment.getDocumentControlNumber()).isEqualTo(dcn);
        assertThat(payment.getPaymentMethod().getName()).isEqualTo("cheque");
        assertThat(payment.getPaymentChannel().getName()).isEqualTo("bulk scan");
        verifyNoInteractions(restTemplatePaymentGroup);
    }

    /**
     * Test that a strategic bulk-scan payment can be allocated to a service request that has not been cancelled.
     * The payment and its allocation are saved, and the bulk-scan service is asked to mark the payment as processed.
     */
    @Test
    public void strategicBulkScanPaymentForOpenServiceRequestPersistsPaymentAndAllocation() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        String dcn = UUID.randomUUID().toString();
        Map<String, String> params = stubBulkScanProcessing(dcn);
        BulkScanPaymentRequestStrategic request = strategicBulkScanRequest(serviceRequest, dcn);

        String response = restActions.post("%s/bulk-scan-payments-strategic".formatted(paymentGroupPath(serviceRequest)), request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Success"))
            .andExpect(jsonPath("$.payment_group_reference").value(serviceRequest.getPaymentReference()))
            .andExpect(jsonPath("$.payment_allocation", hasSize(1)))
            .andReturn().getResponse().getContentAsString();

        Payment payment = persistedPayment(response, "reference", serviceRequest, "success");
        assertThat(payment.getDocumentControlNumber()).isEqualTo(dcn);
        assertThat(jdbcTemplate.queryForMap(
            "select payment_reference, payment_group_reference, allocation_status from payment_allocation where payment_id = ?",
            payment.getId()))
            .containsEntry("payment_reference", payment.getReference())
            .containsEntry("payment_group_reference", serviceRequest.getPaymentReference())
            .containsEntry("allocation_status", "Allocated");
        verify(restTemplatePaymentGroup).exchange(eq(BULK_SCAN_STATUS_URL), eq(HttpMethod.PATCH),
            any(HttpEntity.class), eq(String.class), eq(params));
    }

    /**
     * Test that a service request that has not been cancelled can start a telephony card payment.
     * The payment is saved against the service request, and the response provides the mocked provider's next URL.
     */
    @Test
    public void telephonyPaymentForOpenServiceRequestIsPersisted() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        String nextUrl = "https://example.com/telephony/next";
        stubTelephonyProvider(nextUrl);
        TelephonyCardPaymentsRequest request = telephonyRequest(serviceRequest);

        String response = restActions.post("%s/telephony-card-payments".formatted(paymentGroupPath(serviceRequest)), request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Initiated"))
            .andExpect(jsonPath("$.payment_group_reference").value(serviceRequest.getPaymentReference()))
            .andExpect(jsonPath("$._links.next_url.href").value(nextUrl))
            .andReturn().getResponse().getContentAsString();

        Payment payment = persistedPayment(response, "payment_reference", serviceRequest, "created");
        assertThat(payment.getPaymentMethod().getName()).isEqualTo("card");
        assertThat(payment.getPaymentChannel().getName()).isEqualTo("telephony");
        assertThat(payment.getPaymentProvider().getName()).isEqualTo(PaymentProvider.PCI_PAL.getName());
        ArgumentCaptor<PaymentServiceRequest> providerRequest = ArgumentCaptor.forClass(PaymentServiceRequest.class);
        verify(pciPalPaymentService).create(providerRequest.capture());
        assertThat(providerRequest.getValue().getPaymentGroupReference()).isEqualTo(serviceRequest.getPaymentReference());
        assertThat(providerRequest.getValue().getPaymentReference()).isEqualTo(payment.getReference());
        assertThat(providerRequest.getValue().getAmount()).isEqualByComparingTo(FEE_AMOUNT);
    }

    /**
     * Test that a remission can be applied to a fee on a service request that has not been cancelled.
     * The remission is saved against the existing fee and service request, and the fee's net amount is reduced.
     */
    @Test
    public void remissionForOpenServiceRequestIsPersistedAndReducesFeeNetAmount() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        PaymentFee fee = serviceRequest.getFees().getFirst();
        String hwfReference = "HWF-%s".formatted(UUID.randomUUID());
        RemissionRequest request = remissionRequest(serviceRequest, hwfReference);

        String response = restActions.post("%s/fees/%d/remissions".formatted(paymentGroupPath(serviceRequest), fee.getId()), request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.payment_group_reference").value(serviceRequest.getPaymentReference()))
            .andExpect(jsonPath("$.fee.id").value(fee.getId()))
            .andExpect(jsonPath("$.remission_reference").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        assertPersistedRemission(response, hwfReference, serviceRequest, fee);
        assertThat(paymentFeeRepository.findById(fee.getId()).orElseThrow().getNetAmount())
            .isEqualByComparingTo("6.00");
    }

    /**
     * Test that the CCD case number identifies the correct service request when two requests share a reference.
     * The remission is saved against the requested fee, and only that fee's net amount is reduced.
     * The service request belonging to the other case remains unchanged.
     */
    @Test
    public void remissionWithDuplicateServiceRequestReferencesIsAppliedToTheRequestedCase() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        PaymentFeeLink otherServiceRequest = saveServiceRequest();
        otherServiceRequest.setPaymentReference(serviceRequest.getPaymentReference());
        otherServiceRequest = paymentFeeLinkRepository.save(otherServiceRequest);
        Map<String, List<Map<String, Object>>> otherBefore = databaseState(otherServiceRequest);
        PaymentFee fee = serviceRequest.getFees().getFirst();
        String hwfReference = "HWF-%s".formatted(UUID.randomUUID());
        RemissionRequest request = remissionRequest(serviceRequest, hwfReference);

        String response = restActions.post(
            "%s/fees/%d/remissions".formatted(paymentGroupPath(serviceRequest), fee.getId()), request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.payment_group_reference").value(serviceRequest.getPaymentReference()))
            .andExpect(jsonPath("$.fee.id").value(fee.getId()))
            .andExpect(jsonPath("$.remission_reference").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        String remissionReference = objectMapper.readTree(response).get("remission_reference").asText();
        Remission remission = remissionRepository.findByRemissionReference(remissionReference).orElseThrow();
        assertThat(remission.getHwfReference()).isEqualTo(hwfReference);
        assertThat(remission.getHwfAmount()).isEqualByComparingTo("4.00");
        assertThat(remission.getPaymentFeeLink().getId()).isEqualTo(serviceRequest.getId());
        assertThat(remission.getFee().getId()).isEqualTo(fee.getId());
        assertThat(paymentFeeRepository.findById(fee.getId()).orElseThrow().getNetAmount())
            .isEqualByComparingTo("6.00");
        assertThat(databaseState(otherServiceRequest)).isEqualTo(otherBefore);
    }

    /**
     * Test that a retrospective remission can be applied to a paid service request that has not been cancelled.
     * The remission is saved against the existing fee and service request, and the original payment is retained.
     */
    @Test
    public void retroRemissionForNonCancelledServiceRequestIsPersistedWithoutChangingThePayment() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        String paymentReference = "RC-%s".formatted(UUID.randomUUID());
        serviceRequest.setPayments(List.of(Payment.paymentWith()
            .reference(paymentReference).amount(FEE_AMOUNT).currency("GBP")
            .ccdCaseNumber(serviceRequest.getCcdCaseNumber())
            .paymentStatus(PaymentStatus.SUCCESS)
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentChannel(PaymentChannel.ONLINE).paymentProvider(PaymentProvider.GOV_PAY).build()));
        serviceRequest = paymentFeeLinkRepository.save(serviceRequest);
        PaymentFee fee = serviceRequest.getFees().getFirst();
        String hwfReference = "HWF-%s".formatted(UUID.randomUUID());
        RetroRemissionRequest request = RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfReference(hwfReference).hwfAmount(new BigDecimal("4.00")).build();

        String response = restActions.post("%s/fees/%d/retro-remission".formatted(paymentGroupPath(serviceRequest), fee.getId()), request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.remission_reference").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        assertPersistedRemission(response, hwfReference, serviceRequest, fee);
        Payment payment = paymentRepository.findByReference(paymentReference).orElseThrow();
        assertThat(payment.getAmount()).isEqualByComparingTo(FEE_AMOUNT);
        assertThat(payment.getPaymentStatus().getName()).isEqualTo("success");
        assertThat(jdbcTemplate.queryForObject("select count(*) from payment where payment_link_id = ?",
            Integer.class, serviceRequest.getId())).isEqualTo(1);
    }

    /**
     * Test that the fee identifies the correct service request when two requests share a reference.
     * The retrospective remission is saved against that fee and service request, and the other request remains unchanged.
     */
    @Test
    public void retroRemissionWithDuplicateServiceRequestReferencesIsAppliedToTheRequestedFee() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        PaymentFeeLink otherServiceRequest = saveServiceRequest();
        otherServiceRequest.setPaymentReference(serviceRequest.getPaymentReference());
        otherServiceRequest = paymentFeeLinkRepository.save(otherServiceRequest);
        Map<String, List<Map<String, Object>>> otherBefore = databaseState(otherServiceRequest);
        PaymentFee fee = serviceRequest.getFees().getFirst();
        String hwfReference = "HWF-%s".formatted(UUID.randomUUID());
        RetroRemissionRequest request = RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfReference(hwfReference).hwfAmount(new BigDecimal("4.00")).build();

        String response = restActions.post(
            "%s/fees/%d/retro-remission".formatted(paymentGroupPath(serviceRequest), fee.getId()), request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.remission_reference").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        String remissionReference = objectMapper.readTree(response).get("remission_reference").asText();
        Remission remission = remissionRepository.findByRemissionReference(remissionReference).orElseThrow();
        assertThat(remission.getHwfReference()).isEqualTo(hwfReference);
        assertThat(remission.getHwfAmount()).isEqualByComparingTo("4.00");
        assertThat(remission.getPaymentFeeLink().getId()).isEqualTo(serviceRequest.getId());
        assertThat(remission.getFee().getId()).isEqualTo(fee.getId());
        assertThat(databaseState(otherServiceRequest)).isEqualTo(otherBefore);
    }

    /**
     * Test that another fee cannot be added to a cancelled service request.
     * The response returns HTTP 409 with a cancellation message, and the existing records remain unchanged.
     */
    @Test
    public void addingFeeToCancelledServiceRequestReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);
        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees(List.of(feeRequest("FEE0002", serviceRequest.getCcdCaseNumber())))
            .build();

        assertCancellationRejected(restActions.put(paymentGroupPath(serviceRequest), request), serviceRequest, before);
    }

    /**
     * Test that a bulk-scan payment cannot be added to a cancelled service request.
     * The response returns HTTP 409 with a cancellation message, and no payment or other record is changed.
     */
    @Test
    public void bulkScanPaymentForCancelledServiceRequestReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);
        BulkScanPaymentRequest request = bulkScanRequest(serviceRequest, UUID.randomUUID().toString());

        assertCancellationRejected(restActions.post("%s/bulk-scan-payments".formatted(paymentGroupPath(serviceRequest)), request),
            serviceRequest, before);
    }

    /**
     * Test that a strategic bulk-scan payment cannot be allocated to a cancelled service request.
     * The response returns HTTP 409 with a cancellation message. No payment or allocation is saved,
     * and the bulk-scan service is not asked to mark the payment as processed.
     */
    @Test
    public void strategicBulkScanPaymentForCancelledServiceRequestReturnsConflictWithoutAllocatingOrProcessing() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        String dcn = UUID.randomUUID().toString();
        stubBulkScanProcessing(dcn);
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);
        BulkScanPaymentRequestStrategic request = strategicBulkScanRequest(serviceRequest, dcn);

        assertCancellationRejected(restActions.post("%s/bulk-scan-payments-strategic".formatted(paymentGroupPath(serviceRequest)), request),
            serviceRequest, before);
    }

    /**
     * Test that a cancelled service request cannot start a telephony card payment.
     * The response returns HTTP 409 with a cancellation message. No payment is saved or request sent
     * to the telephony provider, and the existing records remain unchanged.
     */
    @Test
    public void telephonyPaymentForCancelledServiceRequestReturnsConflictWithoutCallingTheProvider() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        stubTelephonyProvider("https://example.com/telephony/next");
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);

        assertCancellationRejected(restActions.post("%s/telephony-card-payments".formatted(paymentGroupPath(serviceRequest)),
            telephonyRequest(serviceRequest)), serviceRequest, before);
    }

    /**
     * Test that a remission cannot be applied to a fee on a cancelled service request.
     * The response returns HTTP 409 with a cancellation message. No remission is saved,
     * and the fee amounts and cancellation details remain unchanged.
     */
    @Test
    public void remissionForCancelledServiceRequestReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        PaymentFee fee = serviceRequest.getFees().getFirst();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);
        RemissionRequest request = remissionRequest(serviceRequest, "HWF-%s".formatted(UUID.randomUUID()));

        assertCancellationRejected(restActions.post("%s/fees/%d/remissions".formatted(paymentGroupPath(serviceRequest), fee.getId()),
            request), serviceRequest, before);
    }

    /**
     * Test that a retrospective remission cannot be applied to a fee on a cancelled service request.
     * The response returns HTTP 409 with a cancellation message, and no remission or other record is changed.
     */
    @Test
    public void retroRemissionForCancelledServiceRequestReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        PaymentFee fee = serviceRequest.getFees().getFirst();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);
        RetroRemissionRequest request = RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfReference("HWF-%s".formatted(UUID.randomUUID())).hwfAmount(new BigDecimal("4.00")).build();

        assertCancellationRejected(restActions.post("%s/fees/%d/retro-remission".formatted(paymentGroupPath(serviceRequest), fee.getId()),
            request), serviceRequest, before);
    }

    /**
     * Test that a fee cannot be deleted from a cancelled service request.
     * The response returns HTTP 409 with a cancellation message, and the fee and service request remain unchanged.
     */
    @Test
    public void deletingFeeForCancelledServiceRequestReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        Integer feeId = serviceRequest.getFees().getFirst().getId();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);

        assertCancellationRejected(restActions.delete("/fees/%d".formatted(feeId)), serviceRequest, before);
    }

    /**
     * Test that case details cannot be changed through a payment belonging to a cancelled service request.
     * The response returns HTTP 409 with a cancellation message, and both the payment and service request remain unchanged.
     */
    @Test
    public void updatingCaseDetailsForCancelledServiceRequestReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequestWithFailedPbaPayment();
        String reference = serviceRequest.getPayments().getFirst().getReference();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);
        Map<String, String> request = Map.of(
            "ccd_case_number", "1111222233334444",
            "case_reference", "updated-case-reference");

        assertCancellationRejected(restActions.patch("/payments/%s".formatted(reference), request), serviceRequest, before);
    }

    /**
     * Test that a failed payment on a cancelled service request cannot be changed to successful through the status endpoint.
     * The response returns HTTP 409 with a cancellation message, and the payment remains failed without changing other records.
     */
    @Test
    public void updatingPaymentStatusForCancelledServiceRequestReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequestWithFailedPbaPayment();
        String reference = serviceRequest.getPayments().getFirst().getReference();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);

        assertCancellationRejected(restActions.patch("/payments/%s/status/success".formatted(reference)), serviceRequest, before);
    }

    /**
     * Test that deleting a PBA payment cannot remove a cancelled service request through cascading deletion.
     * The response returns HTTP 409 with a cancellation message, and the payment, service request and its fees remain unchanged.
     */
    @Test
    public void deletingPbaPaymentForCancelledServiceRequestReturnsConflictWithoutDeletingTheServiceRequest() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequestWithFailedPbaPayment();
        String reference = serviceRequest.getPayments().getFirst().getReference();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);

        assertCancellationRejected(restActions.delete("/credit-account-payments/%s".formatted(reference)), serviceRequest, before);
    }

    /**
     * Test that refund resubmission cannot change a remission on a cancelled service request.
     * The payment belongs to a different, open service request, so the check must use the fee's owner.
     * The response returns HTTP 409, and both service requests and their records remain unchanged.
     */
    @Test
    public void resubmittingRefundForCancelledFeeOwnerReturnsConflictWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink cancelledServiceRequest = saveCancelledServiceRequest();
        Remission remission = saveRemission(cancelledServiceRequest);
        PaymentFeeLink paymentServiceRequest = saveServiceRequestWithPbaPayment(saveServiceRequest(), PaymentStatus.SUCCESS);
        String paymentReference = paymentServiceRequest.getPayments().getFirst().getReference();
        Map<String, List<Map<String, Object>>> cancelledBefore = databaseState(cancelledServiceRequest);
        Map<String, List<Map<String, Object>>> paymentBefore = databaseState(paymentServiceRequest);
        ResubmitRefundRemissionRequest request = ResubmitRefundRemissionRequest.resubmitRefundRemissionRequestWith()
            .refundReason("RR036").feeId(remission.getFee().getId().toString())
            .amount(new BigDecimal("3.00")).totalRefundedAmount(new BigDecimal("3.00")).build();

        assertCancellationRejected(restActions.withAuthorizedUser(UserResolverBackdoor.REFUND_ID)
                .withUserId(UserResolverBackdoor.REFUND_ID)
                .patch("/refund/resubmit/%s".formatted(paymentReference), request),
            cancelledServiceRequest, cancelledBefore);
        assertThat(databaseState(paymentServiceRequest)).isEqualTo(paymentBefore);
    }

    /**
     * Test that refund resubmission can still update a remission on an open service request.
     * The response returns HTTP 200, and the revised remission amount is committed to the database.
     */
    @Test
    public void resubmittingRefundForOpenFeeOwnerUpdatesTheRemission() throws Exception {
        PaymentFeeLink serviceRequest = saveServiceRequestWithPbaPayment(saveServiceRequest(), PaymentStatus.SUCCESS);
        Remission remission = saveRemission(serviceRequest);
        String paymentReference = serviceRequest.getPayments().getFirst().getReference();
        ResubmitRefundRemissionRequest request = ResubmitRefundRemissionRequest.resubmitRefundRemissionRequestWith()
            .refundReason("RR036").feeId(remission.getFee().getId().toString())
            .amount(new BigDecimal("3.00")).totalRefundedAmount(new BigDecimal("3.00")).build();

        restActions.withAuthorizedUser(UserResolverBackdoor.REFUND_ID).withUserId(UserResolverBackdoor.REFUND_ID)
            .patch("/refund/resubmit/%s".formatted(paymentReference), request).andExpect(status().isOk());

        assertThat(remissionRepository.findByRemissionReference(remission.getRemissionReference())
            .orElseThrow().getHwfAmount()).isEqualByComparingTo("3.00");
        assertNotCancelled(serviceRequest);
    }

    /**
     * Test that a cancelled service request can still be read so its details remain available.
     * The response returns HTTP 200 with the existing fee, and reading it does not change the database.
     */
    @Test
    public void cancelledServiceRequestRemainsReadableWithoutChangingTheDatabase() throws Exception {
        PaymentFeeLink serviceRequest = saveCancelledServiceRequest();
        Map<String, List<Map<String, Object>>> before = databaseState(serviceRequest);

        restActions.get(paymentGroupPath(serviceRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payment_group_reference").value(serviceRequest.getPaymentReference()))
            .andExpect(jsonPath("$.fees", hasSize(1)))
            .andExpect(jsonPath("$.fees[0].id").value(serviceRequest.getFees().getFirst().getId()));

        assertThat(databaseState(serviceRequest)).isEqualTo(before);
    }

    private BulkScanPaymentRequest bulkScanRequest(PaymentFeeLink serviceRequest, String dcn) {
        return BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(FEE_AMOUNT)
            .paymentMethod(PaymentMethodType.CHEQUE)
            .service(SERVICE_NAME)
            .siteId(SITE_ID)
            .ccdCaseNumber(serviceRequest.getCcdCaseNumber())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentStatus(PaymentStatus.SUCCESS)
            .currency(CurrencyCode.GBP)
            .giroSlipNo("BCH12345")
            .bankedDate(Instant.now().toString())
            .documentControlNumber(dcn)
            .build();
    }

    private BulkScanPaymentRequestStrategic strategicBulkScanRequest(PaymentFeeLink serviceRequest, String dcn) {
        return BulkScanPaymentRequestStrategic.createBulkScanPaymentStrategicWith()
            .amount(FEE_AMOUNT)
            .paymentMethod(PaymentMethodType.CHEQUE)
            .caseType("Divorce_ExceptionRecord")
            .ccdCaseNumber(serviceRequest.getCcdCaseNumber())
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentStatus(PaymentStatus.SUCCESS)
            .currency(CurrencyCode.GBP)
            .giroSlipNo("BCH12345")
            .bankedDate(Instant.now().toString())
            .documentControlNumber(dcn)
            .paymentAllocationDTO(PaymentAllocationDto.paymentAllocationDtoWith()
                .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Allocated").build())
                .userName("Component test user")
                .build())
            .build();
    }

    private Map<String, String> stubBulkScanProcessing(String dcn) {
        when(featureToggler.getBooleanValue("prod-strategic-fix", false)).thenReturn(true);
        when(authTokenGenerator.generate()).thenReturn("component-service-token");
        Map<String, String> params = Map.of("dcn", dcn, "status", "PROCESSED");
        when(restTemplatePaymentGroup.exchange(eq(BULK_SCAN_STATUS_URL), eq(HttpMethod.PATCH),
            any(HttpEntity.class), eq(String.class), eq(params))).thenReturn(ResponseEntity.ok("processed"));
        return params;
    }

    private TelephonyCardPaymentsRequest telephonyRequest(PaymentFeeLink serviceRequest) {
        return TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(FEE_AMOUNT)
            .ccdCaseNumber(serviceRequest.getCcdCaseNumber())
            .caseType("Divorce_ExceptionRecord")
            .currency(CurrencyCode.GBP)
            .returnURL("https://example.com/payment-return")
            .telephonySystem(KervTelephonySystem.TELEPHONY_SYSTEM_NAME)
            .build();
    }

    private void stubTelephonyProvider(String nextUrl) {
        when(idamService.getUserId(any())).thenReturn(IdamUserIdResponse.idamUserIdResponseWith().sub(USER_ID).build());
        when(pciPalPaymentService.create(any(PaymentServiceRequest.class))).thenReturn(PciPalPayment.pciPalPaymentWith()
            .paymentId(UUID.randomUUID().toString())
            .state(State.stateWith().status("created").build()).build());
        TelephonyProviderAuthorisationResponse providerResponse = new TelephonyProviderAuthorisationResponse();
        providerResponse.setNextUrl(nextUrl);
        providerResponse.setAccessToken("component-access-token");
        providerResponse.setRefreshToken("component-refresh-token");
        when(pciPalPaymentService.getPaymentProviderAuthorisationTokens(any(TelephonySystem.class), anyString()))
            .thenReturn(providerResponse);
        when(pciPalPaymentService.getTelephonyProviderLink(any(PciPalPaymentRequest.class), eq(providerResponse),
            eq(SERVICE_NAME), anyString(), any(TelephonySystem.class))).thenReturn(providerResponse);
    }

    private RemissionRequest remissionRequest(PaymentFeeLink serviceRequest, String hwfReference) {
        return RemissionRequest.createRemissionRequestWith()
            .hwfReference(hwfReference)
            .hwfAmount(new BigDecimal("4.00"))
            .ccdCaseNumber(serviceRequest.getCcdCaseNumber())
            .caseType("Divorce_ExceptionRecord")
            .fee(feeRequest(serviceRequest.getFees().getFirst().getCode(), serviceRequest.getCcdCaseNumber()))
            .build();
    }

    private PaymentFeeLink saveCancelledServiceRequest() {
        PaymentFeeLink serviceRequest = saveServiceRequest();
        serviceRequest.setCancelled(true);
        serviceRequest.setCancellationReason("Auto-Cancelled by Service");
        serviceRequest.setCancelledBy("divorce");
        serviceRequest.setDateCancelled(new Date());
        return paymentFeeLinkRepository.save(serviceRequest);
    }

    private PaymentFeeLink saveCancelledServiceRequestWithFailedPbaPayment() {
        return saveServiceRequestWithPbaPayment(saveCancelledServiceRequest(), PaymentStatus.FAILED);
    }

    private PaymentFeeLink saveServiceRequestWithPbaPayment(PaymentFeeLink serviceRequest, PaymentStatus status) {
        serviceRequest.setPayments(List.of(Payment.paymentWith()
            .reference("RC-%s".formatted(UUID.randomUUID())).amount(FEE_AMOUNT).currency("GBP")
            .ccdCaseNumber(serviceRequest.getCcdCaseNumber()).serviceType(SERVICE_NAME).siteId(SITE_ID)
            .paymentStatus(status)
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentChannel(PaymentChannel.ONLINE).build()));
        return paymentFeeLinkRepository.save(serviceRequest);
    }

    private Remission saveRemission(PaymentFeeLink serviceRequest) {
        PaymentFee fee = serviceRequest.getFees().getFirst();
        Remission remission = Remission.remissionWith()
            .remissionReference("RM-%s".formatted(UUID.randomUUID()))
            .hwfReference("HWF-%s".formatted(UUID.randomUUID())).hwfAmount(new BigDecimal("4.00"))
            .ccdCaseNumber(serviceRequest.getCcdCaseNumber()).siteId(SITE_ID)
            .fee(fee).paymentFeeLink(serviceRequest).build();
        fee.setRemissions(List.of(remission));
        serviceRequest.setRemissions(List.of(remission));
        paymentFeeLinkRepository.save(serviceRequest);
        return remissionRepository.findByRemissionReference(remission.getRemissionReference()).orElseThrow();
    }

    private void assertCancellationRejected(ResultActions result, PaymentFeeLink serviceRequest,
                                            Map<String, List<Map<String, Object>>> before) throws Exception {
        String response = result.andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();
        assertThat(response).containsIgnoringCase("cancelled");
        assertThat(databaseState(serviceRequest)).as("Rejected requests must not change the database").isEqualTo(before);
        verifyNoInteractions(pciPalPaymentService, restTemplatePaymentGroup, authTokenGenerator);
    }

    private Map<String, List<Map<String, Object>>> databaseState(PaymentFeeLink serviceRequest) {
        return Map.of(
            "service_request", jdbcTemplate.queryForList("select * from payment_fee_link where id = ?", serviceRequest.getId()),
            "fees", jdbcTemplate.queryForList("select * from fee where payment_link_id = ? order by id", serviceRequest.getId()),
            "payments", jdbcTemplate.queryForList("select * from payment where payment_link_id = ? order by id", serviceRequest.getId()),
            "remissions", jdbcTemplate.queryForList("select * from remission where payment_link_id = ? order by id", serviceRequest.getId()),
            "allocations", jdbcTemplate.queryForList(
                "select pa.* from payment_allocation pa join payment p on p.id = pa.payment_id where p.payment_link_id = ? order by pa.id",
                serviceRequest.getId()),
            "status_history", jdbcTemplate.queryForList(
                "select sh.* from status_history sh join payment p on p.id = sh.payment_id where p.payment_link_id = ? order by sh.id",
                serviceRequest.getId()),
            "audit", jdbcTemplate.queryForList("select * from payment_audit_history where ccd_case_no = ? order by id",
                serviceRequest.getCcdCaseNumber()));
    }

    private PaymentFeeLink saveServiceRequest() {
        String ccdCaseNumber = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L, 10_000_000_000_000_000L));
        return paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2026-%d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 10_000_000_000_000L)))
            .ccdCaseNumber(ccdCaseNumber).enterpriseServiceName(SERVICE_NAME).orgId(SITE_ID)
            .fees(List.of(PaymentFee.feeWith().code("FEE0001").version("1").volume(1)
                .calculatedAmount(FEE_AMOUNT).amountDue(FEE_AMOUNT).netAmount(FEE_AMOUNT)
                .ccdCaseNumber(ccdCaseNumber).build()))
            .build());
    }

    private FeeDto feeRequest(String code, String ccdCaseNumber) {
        return FeeDto.feeDtoWith().code(code).version("1").volume(1)
            .calculatedAmount(FEE_AMOUNT).ccdCaseNumber(ccdCaseNumber).build();
    }

    private String paymentGroupPath(PaymentFeeLink serviceRequest) {
        return "/payment-groups/%s".formatted(serviceRequest.getPaymentReference());
    }

    private Payment persistedPayment(String response, String referenceField, PaymentFeeLink serviceRequest, String expectedStatus)
        throws Exception {
        String reference = objectMapper.readTree(response).get(referenceField).asText();
        Payment payment = paymentRepository.findByReference(reference).orElseThrow();
        assertThat(payment.getAmount()).isEqualByComparingTo(FEE_AMOUNT);
        assertThat(payment.getCurrency()).isEqualTo("GBP");
        assertThat(payment.getPaymentStatus().getName()).isEqualTo(expectedStatus);
        assertThat(payment.getCcdCaseNumber()).isEqualTo(serviceRequest.getCcdCaseNumber());
        assertThat(payment.getPaymentLink().getId()).isEqualTo(serviceRequest.getId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from payment where payment_link_id = ?",
            Integer.class, serviceRequest.getId())).isEqualTo(1);
        assertNotCancelled(serviceRequest);
        return payment;
    }

    private void assertPersistedRemission(String response, String hwfReference, PaymentFeeLink serviceRequest, PaymentFee fee)
        throws Exception {
        String reference = objectMapper.readTree(response).get("remission_reference").asText();
        Remission remission = remissionRepository.findByRemissionReference(reference).orElseThrow();
        assertThat(remission.getHwfReference()).isEqualTo(hwfReference);
        assertThat(remission.getHwfAmount()).isEqualByComparingTo("4.00");
        assertThat(remission.getPaymentFeeLink().getId()).isEqualTo(serviceRequest.getId());
        assertThat(remission.getFee().getId()).isEqualTo(fee.getId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from remission where payment_link_id = ?",
            Integer.class, serviceRequest.getId())).isEqualTo(1);
        assertNotCancelled(serviceRequest);
    }

    private void assertNotCancelled(PaymentFeeLink serviceRequest) {
        assertThat(paymentFeeLinkRepository.findByPaymentReference(serviceRequest.getPaymentReference())
            .orElseThrow().isCancelled()).isFalse();
    }
}
