package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class AccountControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService<AccountDto, String> accountService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("MM/dd/yyyy");

    private static final String USER_ID = UserResolverBackdoor.CASEWORKER_ID;

    RestActions restActions;

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);
        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");
    }

    @Test
    public void getAccountsWithValidAccountNumber() throws Exception {
        AccountDto expectedResponse = AccountDto.accountDtoWith()
                                    .accountName("account-name")
                                    .accountNumber("account-number")
                                    .status(AccountStatus.ACTIVE)
                                    .availableBalance(BigDecimal.valueOf(100))
                                    .build();
        when(accountService.retrieve(any(String.class))).thenReturn(expectedResponse);
        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/accounts/account-number")
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    public void testAccountNotFoundExceptionWithInvalidAccountNumber() throws Exception {
        when(accountService.retrieve(any(String.class))).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/accounts/invalid-account-number")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void testLiberataServiceUnavailableWhenServiceIsUnavailable() throws Exception {
        when(accountService.retrieve(any(String.class))).thenThrow(new AccountServiceUnavailableException("Liberata Unavailable"));
        MvcResult result = restActions
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .get("/accounts/invalid-account-number")
            .andExpect(status().isServiceUnavailable())
            .andReturn();
    }
}
