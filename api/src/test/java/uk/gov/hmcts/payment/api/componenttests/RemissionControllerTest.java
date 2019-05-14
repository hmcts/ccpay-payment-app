package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.auth.In;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class RemissionControllerTest {

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    private final static String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";

    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    protected RemissionDbBackdoor remissionDbBackdoor;

    @Autowired
    protected PaymentDbBackdoor paymentDbBackdoor;

    @Autowired
    protected PaymentFeeDbBackdoor paymentFeeDbBackdoor;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SiteService<Site, String> siteServiceMock;

    @Before
    public void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");

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
        assertEquals(remissionDto.getCaseReference(), savedRemission.getCaseReference());
        assertEquals(remissionDto.getHwfReference(), savedRemission.getHwfReference());
    }

    @Test
    @Transactional
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
    public void createUpfrontRemissionTest() throws Exception {
        RemissionRequest remissionRequest = getRemissionRequest();

        MvcResult result = restActions
            .post("/remissions", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), RemissionDto.class);
        assertThat(remissionDto).isNotNull();
        assertThat(remissionDto.getRemissionReference()).startsWith("RM");
    }

    @Test
    @Transactional
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
        assertThat(createRemissionResponseDto.getPaymentGroupReference()).isEqualTo(createPaymentResponseDto.getPaymentGroupReference());
        assertThat(createRemissionResponseDto.getPaymentReference()).isEqualTo(createPaymentResponseDto.getReference());
        assertThat(paymentFeeLink.getFees().size()).isEqualTo(1);
    }

    @Test
    public void createRetrospectiveRemissionWithInvalidPaymentGroupReferenceShouldFailTest() throws Exception {
        restActions
            .post("/payment-groups/2019-0000000001/fees/1/remissions" + getRemissionRequest())
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
}
