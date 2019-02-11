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
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;

import static junit.framework.TestCase.assertTrue;
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
            .paymentGroupReference("2018-1234")
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
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .paymentGroupReference("2018-1234")
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
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .paymentGroupReference("2018-1234")
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
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference(hwfReference)
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
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
            .paymentGroupReference("2018-1234")
            .build();

        MvcResult result = restActions
            .post("/remission", remission)
            .andExpect(status().isCreated())
            .andReturn();

        String returnedRemissionReference = result.getResponse().getContentAsString();
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

        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.create(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupReference));

        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .fee(feeDto)
            .paymentGroupReference(paymentGroupReference)
            .build();

        restActions.post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentFee paymentFee = paymentFeeDbBackdoor.findByReference(feeReference);
        assertNotNull(paymentFee);
        assertEquals("new fee refers to the correct payment fee link", paymentFeeLink.getId(), paymentFee.getPaymentLink().getId());
    }

    // TODO: no paymentGroupReference sent -> check payment fee link created and fee created
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
            .fee(feeDto)
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        String returnedRemissionReference = result.getResponse().getContentAsString();
        Remission remission = remissionDbBackdoor.findByRemissionReference(returnedRemissionReference);
        assertNotNull(remission);
        String paymentGroupReference = remission.getPaymentGroupReference();
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
            .build();

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        String returnedRemissionReference = result.getResponse().getContentAsString();
        Remission remission = remissionDbBackdoor.findByRemissionReference(returnedRemissionReference);
        assertNotNull(remission);
        String paymentGroupReference = remission.getPaymentGroupReference();
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.findByReference(paymentGroupReference);
        assertNotNull(paymentFeeLink);
    }

    @Test
    @Transactional
    public void noFeeWithPaymentGroupReferenceAndRemissionGetsCreated() throws Exception {
        String paymentGroupReference = "testGroupReference";

        RemissionRequest remissionRequest = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .hwfReference("HWFref")
            .paymentGroupReference(paymentGroupReference)
            .build();

        paymentDbBackdoor.create(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupReference));

        MvcResult result = restActions
            .post("/remission", remissionRequest)
            .andExpect(status().isCreated())
            .andReturn();

        String returnedRemissionReference = result.getResponse().getContentAsString();
        Remission remission = remissionDbBackdoor.findByRemissionReference(returnedRemissionReference);
        assertNotNull(remission);
        String foundPaymentGroupReference = remission.getPaymentGroupReference();
        assertEquals("Group reference are equal", paymentGroupReference, foundPaymentGroupReference);
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.findByReference(paymentGroupReference);
        assertNotNull(paymentFeeLink);
    }
}
