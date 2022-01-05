package uk.gov.hmcts.payment.api.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.dto.PBAResponse;
import uk.gov.hmcts.payment.api.dto.UserIdentityDataDto;
import uk.gov.hmcts.payment.api.service.IdamService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public class PbaControllerTest extends PaymentsDataUtil {

    private static final String USER_ID = UserResolverBackdoor.SOLICITOR_ID;
    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    protected PaymentDbBackdoor db;
    RestActions restActions;
    MockMvc mvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdamService idamService;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    @Qualifier("restTemplateIdam")
    private RestTemplate restTemplateIdam;

    @MockBean
    @Qualifier("restTemplateRefData")
    private RestTemplate restTemplateRefData;

    @Mock
    private UserIdentityDataDto userIdentityDataDto;

    @Mock
    private MultiValueMap<String, String> map;

    public static final String IDAM_USER_ID = "1f2b7025-0f91-4737-92c6-b7a9baef14c6";
    public static final String IDAM_EMAIL_ID = "test@test.com";

    @Before
    public void setup() {
        mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }

    @After
    public void tearDown() {
        this.restActions = null;
        mvc = null;
    }

    @Test
    @Transactional
    public void searchCreditPayments_withPbaNumber() throws Exception {

        populateCardPaymentToDb("1");
        populateCreditAccountPaymentToDb("2");

        MvcResult result = restActions
            .get("/pba-accounts/123456/payments")
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentsResponse.class);

        assertPbaPayments(paymentsResponse.getPayments());

    }

    @Test
    @Transactional
    public void searchCreditPayments_whichHaveNetAmountAsCalculatedAmount() throws Exception {
        BigDecimal calculatedAmount = new BigDecimal("13.33");
        BigDecimal netAmount = new BigDecimal("23.33");
        populateCreditAccountPaymentToDbWithNetAmountForFee("1", calculatedAmount, netAmount);

        MvcResult result = restActions
            .get("/pba-accounts/123456/payments")
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentsResponse.class);

        assertEquals(1, paymentsResponse.getPayments().size());
        assertEquals(netAmount, paymentsResponse.getPayments().get(0).getFees().get(0).getCalculatedAmount());
        assertNotNull("net_amount should be set", paymentsResponse.getPayments().get(0).getFees().get(0).getNetAmount());
    }

    @Test
    @Transactional
    public void getPBAAccountsFromRefDataSuccess() throws Exception {

        UserIdentityDataDto userIdentityDataDto = UserIdentityDataDto.userIdentityDataWith()
            .emailId("j@mail.com")
            .fullName("ccd-full-name")
            .build();

        when(idamService.getUserId(any())).thenReturn(IDAM_EMAIL_ID);
        when(idamService.getUserIdentityData(any(), any())).thenReturn(userIdentityDataDto);

        PBAResponse mockIdamUserIdResponse = PBAResponse.pbaDtoWith().organisationEntityResponse(null)
            .build();

        ResponseEntity<PBAResponse> responseEntity = new ResponseEntity<>(mockIdamUserIdResponse, HttpStatus.OK);

        when(restTemplateRefData.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(PBAResponse.class)
        )).thenReturn(responseEntity);

        MvcResult result = restActions
            .get("/pba-accounts")
            .andExpect(status().isOk())
            .andReturn();
        PBAResponse pbaResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PBAResponse.class);
        assertNotNull(pbaResponse);

    }

    @Test
    @Transactional
    public void getPBAAccountsFromRefDataFailureForRuntimeException() throws Exception {

        UserIdentityDataDto userIdentityDataDto = UserIdentityDataDto.userIdentityDataWith()
            .emailId("j@mail.com")
            .fullName("ccd-full-name")
            .build();

        when(idamService.getUserId(any())).thenReturn(IDAM_EMAIL_ID);
        when(idamService.getUserIdentityData(any(), any())).thenReturn(userIdentityDataDto);
        when(restTemplateRefData.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(PBAResponse.class)
        )).thenThrow(new RuntimeException());

        MvcResult result = restActions
            .get("/pba-accounts")
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @Transactional
    public void getPBAAccountsFromRefDataFailureForBadRequest() throws Exception {

        UserIdentityDataDto userIdentityDataDto = UserIdentityDataDto.userIdentityDataWith()
            .emailId("j@mail.com")
            .fullName("ccd-full-name")
            .build();

        when(idamService.getUserId(any())).thenReturn(IDAM_EMAIL_ID);
        when(idamService.getUserIdentityData(any(), any())).thenReturn(userIdentityDataDto);
        when(restTemplateRefData.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(PBAResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "internal server request"));

        MvcResult result = restActions
            .get("/pba-accounts")
            .andExpect(status().isInternalServerError())
            .andReturn();
    }


    @Test
    @Transactional
    public void getPBAAccountsFromRefDataFailureForNotFound() throws Exception {

        UserIdentityDataDto userIdentityDataDto = UserIdentityDataDto.userIdentityDataWith()
            .emailId("j@mail.com")
            .fullName("ccd-full-name")
            .build();

        when(idamService.getUserId(any())).thenReturn(IDAM_EMAIL_ID);
        when(idamService.getUserIdentityData(any(), any())).thenReturn(userIdentityDataDto);
        when(restTemplateRefData.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(PBAResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "No PBA Accounts found"));

        MvcResult result = restActions
            .get("/pba-accounts")
            .andExpect(status().isNotFound())
            .andReturn();
    }

}
