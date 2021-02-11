package uk.gov.hmcts.payment.api.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccountServiceUnavailableExceptionTest {
    private void throwException(){
        throw new AccountServiceUnavailableException("Account service unavailable");
    }

    @Test(expected = AccountServiceUnavailableException.class)
    public void checkingIfExceptionThrown(){
        throwException();
    }

    @Test
    public void checkingMessageThrownByException() {
        try {
            throwException();
        } catch (AccountServiceUnavailableException e) {
            assertEquals("Account service unavailable", e.getMessage());
        }

    }
}
