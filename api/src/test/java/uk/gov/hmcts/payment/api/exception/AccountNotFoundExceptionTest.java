package uk.gov.hmcts.payment.api.exception;

import org.junit.Test;

public class AccountNotFoundExceptionTest {

    private void throwException(){
        throw new AccountNotFoundException("Account not found");
    }

    @Test(expected = AccountNotFoundException.class)
    public void checkingIfExceptionThrown(){
        throwException();
    }



}
