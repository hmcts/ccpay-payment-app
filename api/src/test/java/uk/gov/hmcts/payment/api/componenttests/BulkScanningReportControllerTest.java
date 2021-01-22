package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.SecurityUtils;
import uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilter;
import uk.gov.hmcts.payment.api.configuration.security.ServicePaymentFilter;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilterTest.getUserInfoBasedOnUID_Roles;
import static uk.gov.hmcts.payment.api.model.PaymentFee.feeWith;
@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@EnableFeignClients
@AutoConfigureMockMvc
@Transactional
public class BulkScanningReportControllerTest extends PaymentsDataUtil{

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("MM/dd/yyyy");

    @MockBean
    private PaymentService<PaymentFeeLink, String> paymentService;

    private RestActions restActions;

    @MockBean
    private SecurityUtils securityUtils;

    @Autowired
    private ServicePaymentFilter servicePaymentFilter;

    @Autowired
    private ServiceAuthFilter serviceAuthFilter;

    @InjectMocks
    private ServiceAndUserAuthFilter serviceAndUserAuthFilter;

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        when(securityUtils.getUserInfo()).thenReturn(getUserInfoBasedOnUID_Roles("user123","payments"));
        this.restActions = new RestActions(mvc, objectMapper);
        restActions
            .withAuthorizedService("divorce")
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldGenerateReportWhenPaymentProviderIsExela() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("exela").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .build();
        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().isOk())
            .andReturn();

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldGenerateEmptyReportWhenPaymentProviderIsNotExela() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .build();
        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().isOk())
            .andReturn();

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldGenerateEmptyReportWhenPaymentProviderIsExelaAndAllocationStatusIsUnidentified() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Unidentified").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .build();
        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().isOk())
            .andReturn();

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldNotGenerateReportWhenReportTypeIsNotSupported() throws Exception {

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);
        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=DATA_LOSS")
            .andExpect(status().is5xxServerError())
            .andReturn();

    }
    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldNotGenerateReportWhenDateFormatIsNotSupported() throws Exception {

        String startDate = "12345";
        String endDate = "12345";
        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().is5xxServerError())
            .andReturn();

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldNotGenerateReportWhenPaymentIsEmpty() throws Exception {

        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(Collections.emptyList());

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().isOk())
            .andReturn();

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldGenerateReportWhenBulkScanningReportIsAvailable() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("exela").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .build();
        List<Payment> paymentList = new ArrayList<>();

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        paymentList.add(payment);
        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);


        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().isOk())
            .andReturn();

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldNotGenerateReportWhenPaymentAllocationStatusIsInvalid() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred1").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("exela").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .build();
        List<Payment> paymentList = new ArrayList<>();

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        paymentList.add(payment);
        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);


        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().isOk())
            .andReturn();

    }


    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldGenerateReportWhenReportTypeIsSurplus() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Allocated").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("exela").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .dateCreated(new Date())
            .build();
        PaymentFee fee = feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0005").volume(1).build();
        RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber("1111-2222-2222-1111")
            .hwfAmount(new BigDecimal("50.00"))
            .hwfReference("HR1111")
            .siteId("AA001")
            .build();
       Remission remission= Remission.remissionWith()
            .remissionReference("12345")
            .hwfReference("HR1111")
            .hwfAmount(new BigDecimal("50.00"))
            .ccdCaseNumber("1111-2222-2222-1111")
            .siteId("AA001")
            .build();
        List<Payment> paymentList = new ArrayList<>();
        PaymentFeeLink paymentFeeLink = new PaymentFeeLink();
        List<PaymentFee> fees = new ArrayList<>();
        List<Remission> remissions = new ArrayList<>();
        remissions.add(remission);
        fees.add(fee);
        paymentFeeLink.setPayments(paymentList);
        paymentFeeLink.setFees(fees);
        paymentFeeLink.setRemissions(remissions);
        payment.setPaymentLink(paymentFeeLink);
        paymentList.add(payment);

        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=SURPLUS_AND_SHORTFALL")
            .andExpect(status().isOk())
            .andReturn();

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldGenerateReportWhenReportTypeIsSurplusAndStatusIsSuccess() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Allocated").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("exela").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .dateCreated(new Date())
            .build();
        PaymentFee fee = feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0005").volume(1).build();
        RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber("1111-2222-2222-1111")
            .hwfAmount(new BigDecimal("50.00"))
            .hwfReference("HR1111")
            .siteId("AA001")
            .build();
        Remission remission= Remission.remissionWith()
            .remissionReference("12345")
            .hwfReference("HR1111")
            .hwfAmount(new BigDecimal("50.00"))
            .ccdCaseNumber("1111-2222-2222-1111")
            .siteId("AA001")
            .build();
        List<Payment> paymentList = new ArrayList<>();
        PaymentFeeLink paymentFeeLink = new PaymentFeeLink();
        List<PaymentFee> fees = new ArrayList<>();
        List<Remission> remissions = new ArrayList<>();
        remissions.add(remission);
        fees.add(fee);
        paymentFeeLink.setPayments(paymentList);
        paymentFeeLink.setFees(fees);
        paymentFeeLink.setRemissions(remissions);
        payment.setPaymentLink(paymentFeeLink);
        paymentList.add(payment);

        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=SURPLUS_AND_SHORTFALL")
            .andExpect(status().isOk())
            .andReturn();

    }


    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void shouldNotGenerateReportWhenPaymentProviderIsNotExela() throws Exception {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .build();
        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        when(paymentService.getPayments(any(Date.class),any(Date.class))).thenReturn(paymentList);

        String startDate = LocalDate.now().minusDays(1).toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .withAuthorizedUser()
            .get("/payment/bulkscan-data-report?date_from=" + startDate + "&date_to=" + endDate + "&report_type=PROCESSED_UNALLOCATED")
            .andExpect(status().isOk())
            .andReturn();

    }
}
