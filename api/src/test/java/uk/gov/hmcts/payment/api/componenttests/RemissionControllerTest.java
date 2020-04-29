package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.payment.api.configuration.SecurityUtils;
import uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilter;
import uk.gov.hmcts.payment.api.configuration.security.ServicePaymentFilter;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilterTest.getUserInfoBasedOnUID_Roles;

@RunWith(SpringRunner.class)
@ActiveProfiles({"componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@EnableFeignClients
@AutoConfigureMockMvc
public class RemissionControllerTest {

    private final static String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";

    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected RemissionDbBackdoor remissionDbBackdoor;

    @Autowired
    protected PaymentDbBackdoor paymentDbBackdoor;

    @Autowired
    protected PaymentFeeDbBackdoor paymentFeeDbBackdoor;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private SiteService<Site, String> siteServiceMock;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ServiceAuthFilter serviceAuthFilter;

    @Autowired
    private ServicePaymentFilter servicePaymentFilter;

    @InjectMocks
    private ServiceAndUserAuthFilter serviceAndUserAuthFilter;

    @MockBean
    private SecurityUtils securityUtils;

    @Before
    public void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions  = new RestActions(mvc, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withReturnUrl("https://www.gooooogle.com");
        when(securityUtils.getUserInfo()).thenReturn(getUserInfoBasedOnUID_Roles("UID123","payments"));
        List<Site> serviceReturn = Arrays.asList(Site.siteWith()
                .sopReference("sop")
                .siteId("AA99")
                .name("name")
                .service("service")
                .id(1)
                .build(),
            Site.siteWith()
                .sopReference("sop")
                .siteId("AA001")
                .name("name")
                .service("service")
                .id(1)
                .build()
        );

        when(siteServiceMock.getAllSites()).thenReturn(serviceReturn);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void correctAndValidRemissionDataShouldReturn201() throws Exception {
        RemissionRequest remission = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .siteId("AA001")
            .fee(getFee())
            .build();

        restActions
            .post("/remission", remission)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void correctAndValidRemissionDataShouldSaveToDb() {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .fee(getFee())
            .build();

        restActions
            .post("/remission", remissionDto)
            .andReturn();

        Remission savedRemission = remissionDbBackdoor.findByHwfReference(hwfReference);
        assertEquals(remissionDto.getCcdCaseNumber(), savedRemission.getCcdCaseNumber());
        assertEquals(remissionDto.getCaseReference(), savedRemission.getCaseReference());
        assertEquals(remissionDto.getHwfReference(), savedRemission.getHwfReference());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void correctAndValidRemissionWithoutCCDNumberInFeeDataShouldSaveToDb() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionRequest = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .fee(getFeeWithOutCCDCaseNumber())
            .build();

        MvcResult result =  restActions
            .post("/remission", remissionRequest)
            .andReturn();

        RemissionDto remissionResultDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertEquals(remissionRequest.getCcdCaseNumber(),remissionResultDto.getFee().getCcdCaseNumber());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void duplicatehwfReferenceRemissionShouldReturn201() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .fee(getFee())
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isCreated())
            .andReturn();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void emptyHwfReferenceShouldReturn400() throws Exception {
        String hwfReference = "";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void nullHwfReferenceShouldReturn400() throws Exception {
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void nullHwfAmountShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void negativeHwfAmountShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("-10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void hwfAmountEqualToZeroShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("0.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void hwfAmountWithMoreThan2DecimalPlacesShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.001"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void emptyCcdCaseNumberShouldReturn201() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .fee(getFee())
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void nullCcdCaseNumberShouldReturn201() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .fee(getFee())
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void emptyCaseReferenceShouldReturn201() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .fee(getFee())
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void nullCaseReferenceShouldReturn201() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .fee(getFee())
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void emptyCaseReferenceAndEmptyCcdCaseNumberCCDShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("")
            .ccdCaseNumber("")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void nullCaseReferenceAndNullCcdCaseNumberCCDShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void nullCaseReferenceAndEmptyCcdCaseNumberCCDShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .ccdCaseNumber("")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void emptyCaseReferenceAndNullCcdCaseNumberCCDShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .siteId("AA001")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void getRemissionRequestUponSuccessfulCreation() throws Exception {
        RemissionRequest remission = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .siteId("AA001")
            .fee(getFee())
            .build();

        MvcResult result = restActions
            .post("/remission", remission)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertTrue(remissionDto.getRemissionReference().matches(REMISSION_REFERENCE_REGEX));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void feeDtoFilledGetsFeeSaved() throws Exception {
        BigDecimal calculatedAmount = new BigDecimal("199.99");
        String feeReference = "feeReference";

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(calculatedAmount)
            .code("FEE0001")
            .version("1")
            .reference(feeReference)
            .build();

        RemissionRequest remissionRequest = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .siteId("AA001")
            .hwfReference("HWFref")
            .fee(feeDto)
            .build();

        MvcResult result = restActions.post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);

        Remission remission = remissionDbBackdoor.findByRemissionReference(remissionDto.getRemissionReference());
        PaymentFee paymentFee = paymentFeeDbBackdoor.findByPaymentLinkId(remission.getPaymentFeeLink().getId());
        assertNotNull(paymentFee);
        assertThat(paymentFee.getCode()).isEqualTo("FEE0001");
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void paymentFeeLinkAndFeeCreatedWhenNoPaymentGroupReferenceSent() throws Exception {
        BigDecimal calculatedAmount = new BigDecimal("199.99");
        String feeReference = "feeReference";

        String feeCode = "FEE0001";
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(calculatedAmount)
            .code(feeCode)
            .version("1")
            .reference(feeReference)
            .build();

        RemissionRequest remissionRequest = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .siteId("AA001")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fee(feeDto)
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);

        String returnedRemissionReference = remissionDto.getRemissionReference();
        Remission remission = remissionDbBackdoor.findByRemissionReference(returnedRemissionReference);
        assertNotNull(remission);
        String paymentGroupReference = remission.getPaymentFeeLink().getPaymentReference();
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.findByReference(paymentGroupReference);
        assertNotNull(paymentFeeLink);
        PaymentFee paymentFee = paymentFeeDbBackdoor.findByPaymentLinkId(paymentFeeLink.getId());
        assertNotNull(paymentFee);
        assertEquals("Fee code is correct", feeCode, paymentFee.getCode());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void noFeeAndNoPaymentGroupReferenceAndRemissionGetsCreated() throws Exception {
        RemissionRequest remissionRequest = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .siteId("AA001")
            .hwfReference("HWFref")
            .fee(getFee())
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);
        String returnedRemissionReference = remissionDto.getRemissionReference();
        Remission remission = remissionDbBackdoor.findByRemissionReference(returnedRemissionReference);
        assertNotNull(remission);
        PaymentFeeLink paymentFeeLink = remission.getPaymentFeeLink();
        assertNotNull(paymentFeeLink);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void noFeeWithPaymentGroupReferenceAndRemissionGetsCreated() throws Exception {
        RemissionRequest remissionRequest = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .siteId("AA001")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fee(getFee())
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);
        Remission remission = remissionDbBackdoor.findByRemissionReference(remissionDto.getRemissionReference());
        assertNotNull(remission);
        String foundPaymentGroupReference = remission.getPaymentFeeLink().getPaymentReference();
        assertEquals("Group reference are equal", remissionDto.getPaymentGroupReference(), foundPaymentGroupReference);
        PaymentFeeLink paymentFeeLink = remission.getPaymentFeeLink();
        assertNotNull(paymentFeeLink);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void remissionWithWrongSiteIDShouldNotSaveToDb() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .siteId("AA002")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createRemissionWithoutFeeShouldFailTest() throws Exception {
        RemissionRequest remissionRequest = getRemissionRequest();
        remissionRequest.setFee(null);

        MvcResult result = restActions
            .post("/remissions", remissionRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("fee: must not be null");
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createRemissionWithIncorrectHwfAmountShouldFailTest() throws Exception {
        RemissionRequest remissionRequest = getRemissionRequest();
        remissionRequest.setHwfAmount(new BigDecimal("550"));

        MvcResult result = restActions
            .post("/remissions", remissionRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("hwfAmountInvalid: Hwf amount cannot be greater than calculated amount.");
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createRemissionWithNoCcdCaseNumberAndCaseReferenceShoudFailTest() throws Exception {
        RemissionRequest remissionRequest = getRemissionRequest();
        remissionRequest.setCcdCaseNumber(null);
        remissionRequest.setCaseReference(null);

        MvcResult result = restActions
            .post("/remissions", remissionRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("eitherOneRequired: Either ccdCaseNumber or caseReference is required.");
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createUpfrontRemissionTest() throws Exception {
        RemissionRequest remissionRequest = getRemissionRequest();

        MvcResult result = restActions
            .post("/remissions", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertThat(remissionDto).isNotNull();
        assertThat(remissionDto.getRemissionReference()).startsWith("RM");
        assertThat(remissionRequest.getCcdCaseNumber()).isEqualTo(remissionDto.getFee().getCcdCaseNumber());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void createRetrospectiveRemissionWithValidDataShouldBeSuccessfulTest() throws Exception {
        // create a telephony payment
        MvcResult result1 = restActions
            .post("/card-payments", getCardPaymentRequest())
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto createPaymentResponseDto = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentDto.class);
        Integer feeId = createPaymentResponseDto.getFees().get(0).getId();

        assertThat(createPaymentResponseDto).isNotNull();

        // Get fee id
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.findByReference(createPaymentResponseDto.getPaymentGroupReference());

        // create a partial remission
        MvcResult result2 = restActions
            .post("/payment-groups/" + createPaymentResponseDto.getPaymentGroupReference() + "/fees/" + feeId + "/remissions", getRemissionRequest())
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto createRemissionResponseDto = objectMapper.readValue(result2.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertThat(createRemissionResponseDto).isNotNull();

        assertThat(createRemissionResponseDto.getFee().getCcdCaseNumber()).isEqualTo(createPaymentResponseDto.getFees().get(0).getCcdCaseNumber());
        assertThat(createRemissionResponseDto.getPaymentGroupReference()).isEqualTo(createPaymentResponseDto.getPaymentGroupReference());
        assertThat(createRemissionResponseDto.getPaymentReference()).isEqualTo(createPaymentResponseDto.getReference());
        assertThat(paymentFeeLink.getFees().size()).isEqualTo(1);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void createRemissionWithValidDataShouldBeSuccessfulTest() throws Exception {
        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees( Arrays.asList(getNewFee()))
            .build();

        MvcResult result = restActions
            .post("/payment-groups", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);

        assertThat(paymentGroupDto).isNotNull();
        assertThat(paymentGroupDto.getFees().size()).isNotZero();
        assertThat(paymentGroupDto.getFees().size()).isEqualTo(1);

        Integer feeId = paymentGroupDto.getFees().get(0).getId();

        // create a partial remission
        MvcResult result2 = restActions
            .post("/payment-groups/" + paymentGroupDto.getPaymentGroupReference() + "/fees/" + feeId + "/remissions", getRemissionRequestForNetAmount())
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto createRemissionResponseDto = objectMapper.readValue(result2.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertThat(createRemissionResponseDto).isNotNull();

        MvcResult result3 = restActions
            .get("/payment-groups/" + paymentGroupDto.getPaymentGroupReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto1 = objectMapper.readValue(result3.getResponse().getContentAsByteArray(), PaymentGroupDto.class);

        assertThat(paymentGroupDto1.getFees().get(0).getNetAmount()).isEqualTo(paymentGroupDto.getFees().get(0).getCalculatedAmount().subtract(getRemissionRequestForNetAmount().getHwfAmount()));

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void createRemissionWithoutFeesShouldBeSuccessfulTest() throws Exception {
        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees( Arrays.asList(getNewFee()))
            .build();

        PaymentGroupDto consecutiveRequest = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(getConsecutiveFee())).build();

        MvcResult result = restActions
            .post("/payment-groups", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentGroupDto.class);

        assertThat(paymentGroupDto).isNotNull();
        assertThat(paymentGroupDto.getFees().size()).isNotZero();
        assertThat(paymentGroupDto.getFees().size()).isEqualTo(1);

        Integer feeId = paymentGroupDto.getFees().get(0).getId();

        MvcResult result2 = restActions
            .put("/payment-groups/" + paymentGroupDto.getPaymentGroupReference(), consecutiveRequest)
            .andExpect(status().isOk())
            .andReturn();

        // create a partial remission
        MvcResult result3 = restActions
            .post("/payment-groups/" + paymentGroupDto.getPaymentGroupReference() + "/fees/" + feeId + "/remissions", getRemissionRequestForNetAmount())
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto createRemissionResponseDto = objectMapper.readValue(result3.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertThat(createRemissionResponseDto).isNotNull();

        MvcResult result4 = restActions
            .get("/payment-groups/" + paymentGroupDto.getPaymentGroupReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto1 = objectMapper.readValue(result4.getResponse().getContentAsByteArray(), PaymentGroupDto.class);

        assertThat(paymentGroupDto1.getFees().get(0).getNetAmount()).isEqualTo(paymentGroupDto.getFees().get(0).getCalculatedAmount().subtract(getRemissionRequestForNetAmount().getHwfAmount()));

        MvcResult result5 = restActions
            .post("/payment-groups/" + paymentGroupDto.getPaymentGroupReference() + "/fees/" + feeId + "/remissions", getRemissionRequest())
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto createRemissionResponseDto1 = objectMapper.readValue(result5.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertThat(createRemissionResponseDto1).isNotNull();

    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createRetrospectiveRemissionWithInvalidPaymentGroupReferenceShouldFailTest() throws Exception {
        restActions
            .post("/payment-groups/2019-0000000001/fees/1/remissions" , getRemissionRequest())
            .andExpect(status().isNotFound());
    }


    private RemissionRequest getRemissionRequest() {
        return RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber("1111-2222-3333-4444")
            .hwfAmount(new BigDecimal("20"))
            .hwfReference("HR1111")
            .siteId("AA001")
            .fee(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("300"))
                .code("FEE0111")
                .version("1")
                .volume(1)
                .build())
            .build();
    }

    private CardPaymentRequest getCardPaymentRequest() {
        return CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("250"))
            .ccdCaseNumber("1111-2222-3333-4444")
            .channel("telephony")
            .currency(CurrencyCode.GBP)
            .description("A test telephony payment")
            .provider("pci pal")
            .service(Service.DIVORCE)
            .siteId("AA001")
            .fees(Collections.singletonList(getFee()))
            .build();
    }

    private FeeDto getFee() {
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("10.00"))
            .ccdCaseNumber("CCD1234")
            .version("1")
            .code("FEE0123")
            .build();
    }

    private FeeDto getFeeWithOutCCDCaseNumber() {
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("10.00"))
            .version("1")
            .code("FEE0123")
            .build();
    }

    private FeeDto getNewFee(){
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("250"))
            .code("FEE312")
            .version("1")
            .volume(1)
            .reference("BXsd1123")
            .ccdCaseNumber("1111-2222-2222-1111")
            .build();

    }

    private RemissionRequest getRemissionRequestForNetAmount() {
        return RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber("1111-2222-2222-1111")
            .hwfAmount(new BigDecimal("150"))
            .hwfReference("HR1111")
            .siteId("AA001")
            .fee(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("250"))
                .code("FEE312")
                .version("1")
                .volume(1)
                .build())
            .build();
    }

    private FeeDto getConsecutiveFee() {
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100.19"))
            .code("FEE313")
            .id(1)
            .version("1")
            .volume(2)
            .reference("BXsd11253")
            .ccdCaseNumber("1111-2222-2222-1111")
            .build();
    }
}
