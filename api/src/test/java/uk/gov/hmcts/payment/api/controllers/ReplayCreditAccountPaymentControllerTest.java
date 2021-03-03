package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.controllers.utils.ReplayCreditAccountPaymentUtils;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.ReplayCreditAccountPaymentService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.io.File;
import java.io.FileInputStream;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.util.Files.newFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class ReplayCreditAccountPaymentControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReplayCreditAccountPaymentUtils replayCreditAccountPaymentUtils;

    @MockBean
    private LaunchDarklyFeatureToggler featureToggler;

    @MockBean
    private ReplayCreditAccountPaymentService replayCreditAccountPaymentService;

    @MockBean
    private CreditAccountPaymentController creditAccountPaymentController;

    @MockBean
    private AccountService<AccountDto, String> accountService;

    RestActions restActions;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    private static Logger mockLOG;


    @Before
    public void setup() {

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");
        Mockito.reset(accountService);
        Mockito.reset(replayCreditAccountPaymentService);
        Mockito.reset(creditAccountPaymentController);
        Mockito.reset(replayCreditAccountPaymentService);
        Mockito.reset(featureToggler);


    }

    @Test
    public void shouldMakeReplayCreditAccountPaymentAndReturnsSuccessMessage() throws Exception {
        File paymentsToReplayCSV =null;
        MvcResult result;
        try{
             paymentsToReplayCSV = newFile("src/test/resources/mockPaymentsToReplay.csv");
            Map<String, CreditAccountPaymentRequest> csvParseMap = new HashMap<>();
            createCreditAccountPayments(csvParseMap);
            replayCreditAccountPaymentUtils.createCSV(csvParseMap,"mockPaymentsToReplay.csv");
            MockMultipartFile csvFile = new MockMultipartFile("csvFile", "mockPaymentsToReplay.csv", "text/csv",
                new FileInputStream(new File("src/test/resources/mockPaymentsToReplay.csv")));
            doNothing().when(replayCreditAccountPaymentService).updatePaymentStatusByReference(anyString(),any(PaymentStatus.class), anyString());
            PaymentDto mockPaymentDto = PaymentDto.payment2DtoWith().build();
            ResponseEntity<PaymentDto> responseEntity = ResponseEntity.of(Optional.of(mockPaymentDto));
            when(creditAccountPaymentController.createCreditAccountPayment(any(CreditAccountPaymentRequest.class))).thenReturn(responseEntity);
            result = restActions
                .postWithMultiPartFileData("/replay-credit-account-payments",
                    csvFile, "isReplayPBAPayments", "true")
                .andExpect(status().isOk())
                .andReturn();
        }finally {
            Files.delete(paymentsToReplayCSV);
        }
        assertEquals("Replay Payment Completed Successfully",result.getResponse().getContentAsString());
    }

    @Test
    public void shouldReturnBadRequestWhenRequestMadeWithEmptyFile() throws Exception {
        File emptyCSV =null;
        try {
            emptyCSV = newFile("src/test/resources/emptyCSV.csv");
            MockMultipartFile csvFile = new MockMultipartFile("csvFile", "emptyCSV.csv", "text/csv",
                new FileInputStream(new File("src/test/resources/emptyCSV.csv")));
            restActions
                .postWithMultiPartFileData("/replay-credit-account-payments",
                    csvFile, "isReplayPBAPayments", "true")
                .andExpect(status().isBadRequest())
                .andReturn();
        }finally {
            Files.delete(emptyCSV);
        }
    }

    @Test
    public void shouldSendOkWhenAnExceptionOccuredInPayment() throws Exception {
        File paymentsToReplayCSV =null;
        try{
            paymentsToReplayCSV = newFile("src/test/resources/mockPaymentsToReplay.csv");
            Map<String, CreditAccountPaymentRequest> csvParseMap = new HashMap<>();
            createCreditAccountPayments(csvParseMap);
            replayCreditAccountPaymentUtils.createCSV(csvParseMap,"mockPaymentsToReplay.csv");
            MockMultipartFile csvFile = new MockMultipartFile("csvFile", "mockPaymentsToReplay.csv", "text/csv",
                new FileInputStream(new File("src/test/resources/mockPaymentsToReplay.csv")));
            doThrow(PaymentException.class).when(replayCreditAccountPaymentService).updatePaymentStatusByReference(anyString(),any(PaymentStatus.class), anyString());
            restActions
                .postWithMultiPartFileData("/replay-credit-account-payments",
                    csvFile, "isReplayPBAPayments", "false")
                .andExpect(status().isOk())
                .andReturn();
        }finally {
            Files.delete(paymentsToReplayCSV);
        }
    }


    private void createCreditAccountPayments(Map<String, CreditAccountPaymentRequest> csvParseMap) throws Exception {
            List<FeeDto> fees = new ArrayList<>();
            fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber("ccdCaseNumber").feeAmount(new BigDecimal(20))
                .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
            fees.add(FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber("ccdCaseNumber").feeAmount(new BigDecimal(40))
                .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
            fees.add(FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber("ccdCaseNumber").feeAmount(new BigDecimal(60))
                .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());
            CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
                .amount(new BigDecimal("120"))
                .description("New passport application")
                .caseReference("aCaseReference")
                .ccdCaseNumber("ccdCaseNumber")
                .service(Service.PROBATE)
                .currency(CurrencyCode.GBP)
                .siteId("ABA6")
                .customerReference("CUST101")
                .organisationName("ORG101")
                .accountNumber("accountNumber")
                .fees(fees)
                .build();
            csvParseMap.put("RC-1212-1232-1232-2131", request);

    }
}
