package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockAccountServiceImplTest {

    private final MockAccountServiceImpl accountService = new MockAccountServiceImpl();

    @Test
    void shouldReturnActiveAccountUsingRequestedPbaNumber() {
        AccountDto account = accountService.retrieve("PBAFUNC12345");

        assertEquals("PBAFUNC12345", account.getAccountNumber());
        assertEquals("CAERPHILLY COUNTY BOROUGH COUNCIL", account.getAccountName());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(new BigDecimal("28879"), account.getCreditLimit());
        assertEquals(new BigDecimal("30000"), account.getAvailableBalance());
    }

    @Test
    void shouldReturnProbateAccountAsActive() {
        AccountDto account = accountService.retrieve("PBA0082126");

        assertEquals("PBA0082126", account.getAccountNumber());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    void shouldReturnDeletedAccount() {
        AccountDto account = accountService.retrieve("PBA0087741");

        assertEquals("PBA0087741", account.getAccountNumber());
        assertEquals(AccountStatus.DELETED, account.getStatus());
    }

    @Test
    void shouldReturnOnHoldAccount() {
        AccountDto account = accountService.retrieve("PBA0078303");

        assertEquals("PBA0078303", account.getAccountNumber());
        assertEquals(AccountStatus.ON_HOLD, account.getStatus());
    }

    @Test
    void shouldThrowNotFoundForUnknownAccount() {
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> accountService.retrieve("QWERTYUIOP")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldThrowServerErrorForConfiguredErrorAccount() {
        HttpServerErrorException exception = assertThrows(
            HttpServerErrorException.class,
            () -> accountService.retrieve("PBAFUNC360")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }
}
