package uk.gov.hmcts.payment.api.controllers;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceInaccessibleException;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.GONE;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class AccountControllerTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);
    AccountDto expectedDto = new AccountDto("PBA4324", "accountName", new BigDecimal(100),
        new BigDecimal(100), AccountStatus.ACTIVE, new Date());

    ResponseEntity<AccountStatusError> response;

    @Mock
    private AccountService<AccountDto, String> accountServiceMock;
    @InjectMocks
    private AccountController accountController;

    private static final String STATUS_DELETED = "Deleted";
    private static final String STATUS_ON_HOLD = "On-Hold";

    private static final String JSON_ERROR_MESSAGE_GONE = "The account has been deleted and is no longer available.";
    private static final String JSON_ERROR_MESSAGE_ON_HOLD = "The account is on hold and temporarily unavailable.";

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

    @Test(expected = LiberataServiceInaccessibleException.class)
    public void gettingNonExistingAccountNumberThrowsAccountNotFoundException() {

        //For Same account return account not found
        when(accountServiceMock.retrieve(eq("PBA4321"))).thenThrow(RuntimeException.class);

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

    @Test
    public void gettingNonExistingAccountNumberReturns503() throws Exception {
        String errorMessage = "errorMessage";
        LiberataServiceInaccessibleException ex = new LiberataServiceInaccessibleException(errorMessage);
        assertEquals(errorMessage, accountController.return503(ex));
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
    public void testReturnAccountStatusError_Gone() {

        HttpClientErrorException exception = new HttpClientErrorException(GONE);

        ResponseEntity<Object> response = accountController.returnAccountStatusError(exception);

        assertEquals(GONE, response.getStatusCode());
        assertTrue(response.getBody() instanceof AccountStatusError);
        AccountStatusError error = (AccountStatusError) response.getBody();
        assertEquals(STATUS_DELETED, error.getStatus());
        assertEquals(JSON_ERROR_MESSAGE_GONE, error.getErrorMessage());
    }

    @Test
    public void testReturnAccountStatusError_Precondition_Faild() {

        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.PRECONDITION_FAILED);

        ResponseEntity<Object> response = accountController.returnAccountStatusError(exception);

        assertEquals(HttpStatus.PRECONDITION_FAILED, response.getStatusCode());
        assertTrue(response.getBody() instanceof AccountStatusError);
        AccountStatusError error = (AccountStatusError) response.getBody();
        assertEquals(STATUS_ON_HOLD, error.getStatus());
        assertEquals(JSON_ERROR_MESSAGE_ON_HOLD, error.getErrorMessage());
    }

    @Test
    public void testReturnAccountStatusErrorAccountNotFound() {

        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);

        ResponseEntity<Object> response = accountController.returnAccountStatusError(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
    }


    @Test
    public void whenRetrieveThrowsOtherException_thenWrappedInLiberataServiceInaccessibleException() {
        when(accountServiceMock.retrieve(anyString())).thenThrow(new RuntimeException("Unexpected error"));

        Exception exception = assertThrows(LiberataServiceInaccessibleException.class, () -> accountController.getAccounts("123"));
        assertTrue(exception.getMessage().contains("Failed to connect with Liberata"));
    }

    @Test
    public void whenRetrieveThrowsHttpClientErrorException_thenExceptionIsRethrown() {
        when(accountServiceMock.retrieve(anyString())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(HttpClientErrorException.class, () -> accountController.getAccounts("123"));
    }

}
