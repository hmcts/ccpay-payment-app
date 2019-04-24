package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Before
    public void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    @Transactional
    public void correctAndValidRemissionDataShouldReturn201() throws Exception {
        RemissionRequest remission = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remission)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    public void correctAndValidRemissionDataShouldSaveToDb() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionRequest)
            .andReturn();

        Remission savedRemission = remissionDbBackdoor.findByHwfReference(hwfReference);
        assertEquals(remissionRequest.getCaseReference(), savedRemission.getCaseReference());
        assertEquals(remissionRequest.getHwfReference(), savedRemission.getHwfReference());
    }

    @Test
    @Transactional
    public void duplicatehwfReferenceRemissionShouldReturn201() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @Transactional
    public void emptyHwfReferenceShouldReturn400() throws Exception {
        String hwfReference = "";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void nullHwfReferenceShouldReturn400() throws Exception {
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void nullHwfAmountShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void negativeHwfAmountShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("-10.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void hwfAmountEqualToZeroShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("0.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void hwfAmountWithMoreThan2DecimalPlacesShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.001"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void emptyCcdCaseNumberShouldReturn201() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
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
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
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
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
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
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
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
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("")
            .ccdCaseNumber("")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void nullCaseReferenceAndNullCcdCaseNumberCCDShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void nullCaseReferenceAndEmptyCcdCaseNumberCCDShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .ccdCaseNumber("")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void emptyCaseReferenceAndNullCcdCaseNumberCCDShouldReturn400() throws Exception {
        String hwfReference = "HWFref";
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("")
            .hwfAmount(new BigDecimal("10.01"))
            .hwfReference(hwfReference)
            .fees(Arrays.asList(getFee()))
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void getRemissionRequestUponSuccessfulCreation() throws Exception {
        RemissionRequest remission = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fees(Arrays.asList(getFee()))
            .build();

        MvcResult result = restActions
            .post("/remission", remission)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsString(), RemissionDto.class);
        String returnedRemissionReference = remissionDto.getRemissionReference();
        assertTrue(returnedRemissionReference.matches(REMISSION_REFERENCE_REGEX));
    }

    @Test
    @Transactional
    public void feeDtoFilledGetsFeeSaved() throws Exception {
        BigDecimal calculatedAmount = new BigDecimal("199.99");
        String feeReference = "feeReference";
        String paymentGroupReference = "testGroupReference";

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(calculatedAmount)
            .code("FEE0001")
            .version("1")
            .reference(feeReference)
            .build();

        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fees(Arrays.asList(feeDto))
            .build();

        MvcResult result = restActions.post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsString(), RemissionDto.class);

        Remission remission = remissionDbBackdoor.findByRemissionReference(remissionDto.getRemissionReference());
        PaymentFee paymentFee = remission.getPaymentFeeLink().getFees().get(0);
        assertNotNull(paymentFee);
        assertThat(paymentFee.getCode()).isEqualTo(feeDto.getCode());
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

        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fees(Arrays.asList(feeDto))
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();


        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsString(), RemissionDto.class);

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
        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fees(Arrays.asList(getFee()))
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsString(), RemissionDto.class);

        String returnedRemissionReference = remissionDto.getRemissionReference();
        Remission remission = remissionDbBackdoor.findByRemissionReference(returnedRemissionReference);
        assertNotNull(remission);
        String paymentGroupReference = remission.getPaymentFeeLink().getPaymentReference();
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.findByReference(paymentGroupReference);
        assertNotNull(paymentFeeLink);
    }

    @Test
    @Transactional
    public void noFeeWithPaymentGroupReferenceAndRemissionGetsCreated() throws Exception {
        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fees(Arrays.asList(getFee()))
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();


        RemissionDto remissionDto = objectMapper.readValue(result.getResponse().getContentAsString(), RemissionDto.class);

        String returnedRemissionReference = remissionDto.getRemissionReference();
        Remission remission = remissionDbBackdoor.findByRemissionReference(returnedRemissionReference);
        assertNotNull(remission);
        String foundPaymentGroupReference = remission.getPaymentFeeLink().getPaymentReference();
        assertEquals("Group reference are equal", remissionDto.getPaymentGroupReference(), foundPaymentGroupReference);
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.findByReference(remissionDto.getPaymentGroupReference());
        assertNotNull(paymentFeeLink);
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
