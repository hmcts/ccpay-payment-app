package uk.gov.hmcts.payment.api.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccountNotFoundExceptionTest {

    private void throwException(){
        throw new AccountNotFoundException("Account not found");
    }

    @Test(expected = AccountNotFoundException.class)
    public void checkingIfExceptionThrown(){
        throwException();
    }

    @Test
    public void checkingMessageThrownByException(){
        try{
            throwException();
        }catch (AccountNotFoundException e){
            assertEquals("Account not found",e.getMessage());
        }

    }


}
