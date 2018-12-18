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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class RemissionControllerTest {

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    protected RemissionBackdoor db;

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

        Remission savedRemission = db.findByHwfReference(hwfReference);
        assertEquals(remissionDto.getCaseReference(), savedRemission.getCaseReference());
        assertEquals(remissionDto.getHwfReference(), savedRemission.getHwfReference());
    }

    @Test
    @Transactional
    public void duplicatehwfReferenceRemissionShouldReturn400() throws Exception {
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
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void emptyHwfReferenceShouldReturn422() throws Exception {
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    @Transactional
    public void nullHwfReferenceShouldReturn422() throws Exception {
        RemissionRequest remissionDto = RemissionRequest.createPaymentRecordRequestDtoWith()
            .beneficiaryName("beneficiary")
            .caseReference("caseRef1234")
            .ccdCaseNumber("CCD1234")
            .hwfAmount(new BigDecimal("10.00"))
            .paymentGroupReference("2018-1234")
            .build();

        restActions
            .post("/remission", remissionDto)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    @Transactional
    public void nullHwfAmountShouldReturn422() throws Exception {
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    @Transactional
    public void negativeHwfAmountShouldReturn422() throws Exception {
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    @Transactional
    public void hwfAmountEqualToZeroShouldReturn422() throws Exception {
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    @Transactional
    public void hwfAmountWithMoreThan2DecimalPlacesShouldReturn422() throws Exception {
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    @Transactional
    public void emptyCcdCaseNumberShouldReturn422() throws Exception {
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    @Transactional
    public void nullCcdCaseNumberShouldReturn422() throws Exception {
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }
}
