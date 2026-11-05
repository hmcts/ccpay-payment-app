package uk.gov.hmcts.payment.api.service;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.util.AccountStatus;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceTest {

    @InjectMocks
    private AccountServiceImpl accountService;

    @Mock
    private LiberataRealTimeAPI liberataRealTimeAPI;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<AccountDto> responseEntity;

    @Before
    public void setUp() {
        // Setup mock behavior
        TokenResponse tokenResponse = new TokenResponse("mockAccessToken", 0, 0);
        when(liberataRealTimeAPI.getValidToken()).thenReturn(tokenResponse);
    }

    @Test
    public void testRetrieve_validTestPbaCode() throws Exception {
        // Arrange
        String pbaCode = "PBAFUNC12345";

        // Create expected AccountDto
        AccountDto expectedAccount = AccountDto.accountDtoWith()
            .accountNumber("PBAFUNC12345")
            .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
            .creditLimit(BigDecimal.valueOf(28879))
            .availableBalance(BigDecimal.valueOf(30000))
            .status(AccountStatus.ACTIVE)
            .build();

        // Act
        AccountDto result = accountService.retrieve(pbaCode);

        // Assert
        assertNotNull(result);
        assertEquals(expectedAccount.getAccountNumber(), result.getAccountNumber());
        assertEquals(expectedAccount.getAccountName(), result.getAccountName());
        assertEquals(expectedAccount.getCreditLimit(), result.getCreditLimit());
        assertEquals(expectedAccount.getAvailableBalance(), result.getAvailableBalance());
        assertEquals(expectedAccount.getStatus(), result.getStatus());
    }

    @Test(expected = HttpClientErrorException.class)
    public void testRetrieve_HttpClientErrorException() throws Exception {
        // Arrange
        String pbaCode = "PBA1234332";

        // Simulate a scenario where the restTemplate throws an HttpClientErrorException
        when(liberataRealTimeAPI.getAccountDetails(any(), eq(pbaCode)))
            .thenThrow(HttpClientErrorException.class);

        // Act
        accountService.retrieve(pbaCode);
    }

    @Test(expected = ResourceAccessException.class)
    public void testRetrieve_ResourceAccessException() throws Exception {
        // Arrange
        String pbaCode = "PBA1234332";

        // Simulate a scenario where the restTemplate throws a ResourceAccessException
        when(liberataRealTimeAPI.getAccountDetails(any(), eq(pbaCode)))
            .thenThrow(ResourceAccessException.class);

        // Act
        accountService.retrieve(pbaCode);
    }

    @Test
    public void testRetrieve_externalServiceCall() throws Exception {
        // Arrange
        String pbaCode = "PBAFUNC67890";
        AccountDto externalAccount = new AccountDto(pbaCode, "accountName", new BigDecimal(5000),
            new BigDecimal(7000), AccountStatus.ACTIVE, new Date());

        // Mock restTemplate behavior
        when(liberataRealTimeAPI.getAccountDetails(any(), eq(pbaCode)))
            .thenReturn(externalAccount);

        // Act
        AccountDto result = accountService.retrieve(pbaCode);

        // Assert
        assertNotNull(result);
        assertEquals(externalAccount.getAccountNumber(), result.getAccountNumber());
        assertEquals(externalAccount.getAccountName(), result.getAccountName());
    }
}
