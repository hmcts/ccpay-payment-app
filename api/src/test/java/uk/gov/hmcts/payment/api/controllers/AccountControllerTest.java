package uk.gov.hmcts.payment.api.controllers;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceInaccessibleException;
import uk.gov.hmcts.payment.api.exception.PbaAccountStatusException;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.collections.MapUtils.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class AccountControllerTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);
    AccountDto expectedDto = new AccountDto("PBA4324", "accountName", new BigDecimal(100),
        new BigDecimal(100), AccountStatus.ACTIVE, new Date());
    @Mock
    private AccountService<AccountDto, String> accountServiceMock;
    @InjectMocks
    private AccountController accountController;

    @Test
    public void gettingExistingAccountNumberReturnsAccountDetails() {

        //When Account Matches
        when(accountServiceMock.retrieve(eq("PBA1234"))).thenReturn(expectedDto);

        //When Not Account Matches
        when(accountServiceMock.retrieve(not(eq("PBA1234")))).
            thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "UnKnown test pba account number"));


        AccountDto actualDto = accountController.getAccounts("PBA1234");

        assertEquals(expectedDto, actualDto);
    }

    @Test(expected = AccountNotFoundException.class)
    public void gettingNonExistingAccountNumberThrowsAccountNotFoundException() {

        //For Same account return account not found
        when(accountServiceMock.retrieve(eq("PBA4321"))).thenThrow(HttpClientErrorException.class);

        //For Any other account return account object
        when(accountServiceMock.retrieve(not(eq("PBA4321")))).thenReturn(expectedDto);
        accountController.getAccounts("PBA4321");
    }

    @Test(expected = LiberataServiceInaccessibleException.class)
    public void gettingNonExistingAccountNumberThrowsLiberataServiceInaccessibleException() {
        when(accountServiceMock.retrieve("PBA4321")).thenThrow(LiberataServiceInaccessibleException.class);
        accountController.getAccounts("PBA4321");
    }

    @Test
    public void gettingNonExistingAccountNumberReturns404() throws Exception {
        String errorMessage = "errorMessage";
        AccountNotFoundException ex = new AccountNotFoundException(errorMessage);
        assertEquals(errorMessage, accountController.return404(ex));
    }

    @Test(expected = LiberataServiceInaccessibleException.class)
    public void getLiberataServiceInaccessibleException() {

        //For Same account return Auth exception
        when(accountServiceMock.retrieve(eq("PBA4324"))).thenThrow(OAuth2AccessDeniedException.class);

        //For Any other account return account object
        when(accountServiceMock.retrieve(not(eq("PBA4324")))).thenReturn(expectedDto);
        accountController.getAccounts("PBA4324");
    }


    @Test
    public void testGetAccounts_AccountGone() throws JSONException {
        String accountNumber = "123";
        JSONObject expectedJsonObject = new JSONObject();
        expectedJsonObject.put("status", "Deleted");
        expectedJsonObject.put("error_message", "The account has been deleted and is no longer available.");

        HttpClientErrorException preconditionFailedException = new HttpClientErrorException(HttpStatus.GONE);

        when(accountServiceMock.retrieve(accountNumber)).thenThrow(preconditionFailedException);

        PbaAccountStatusException exception = assertThrows(PbaAccountStatusException.class,
            () -> accountController.getAccounts(accountNumber));

        Map<String, Object> expectedMap = toMap(expectedJsonObject);
        String actualJsonString = exception.getMessage();

        // Parse the actual JSON string into a JSONObject
        JSONObject actualJsonObject = new JSONObject(actualJsonString);

        // Convert the actual JSONObject to a map
        Map<String, Object> actualMap = toMap(actualJsonObject);

        assertEquals(expectedMap,actualMap);
    }
    @Test
    public void testGetAccounts_AccountOnHold() throws JSONException {
        String accountNumber = "123";
        JSONObject expectedJsonObject = new JSONObject();
        expectedJsonObject.put("status", "On-Hold");
        expectedJsonObject.put("error_message", "The account is on hold and temporarily unavailable.");

        HttpClientErrorException preconditionFailedException = new HttpClientErrorException(HttpStatus.PRECONDITION_FAILED);

        when(accountServiceMock.retrieve(accountNumber)).thenThrow(preconditionFailedException);

        PbaAccountStatusException exception = assertThrows(PbaAccountStatusException.class,
            () -> accountController.getAccounts(accountNumber));

        Map<String, Object> expectedMap = toMap(expectedJsonObject);
        String actualJsonString = exception.getMessage();

        // Parse the actual JSON string into a JSONObject
        JSONObject actualJsonObject = new JSONObject(actualJsonString);

        // Convert the actual JSONObject to a map
        Map<String, Object> actualMap = toMap(actualJsonObject);

        assertEquals(expectedMap,actualMap);
    }


    @Test
    public void testGetAccounts_InternalServerError() {
        String accountNumber = "123";
        when(accountServiceMock.retrieve(accountNumber))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
            () -> accountController.getAccounts(accountNumber));

        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    public void testGetAccounts_UnauthorisedError() {
        String accountNumber = "123";
        when(accountServiceMock.retrieve(accountNumber))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
            () -> accountController.getAccounts(accountNumber));

        assertEquals("Account not found", exception.getMessage());
    }


    @Test
    public void testGetAccounts_ForbiddenError() {
        String accountNumber = "123";

        when(accountServiceMock.retrieve(accountNumber))
            .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
            () -> accountController.getAccounts(accountNumber));

        assertEquals("Account not found", exception.getMessage());
    }


    private Map<String, Object> toMap(JSONObject jsonObject) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, jsonObject.get(key));
        }
        return map;
    }

}
