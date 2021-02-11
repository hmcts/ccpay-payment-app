package uk.gov.hmcts.payment.api.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LiberataServiceInaccessibleExceptionTest {
    private void throwException(){
        throw new LiberataServiceInaccessibleException("Liberata service inaccessible");
    }

    @Test(expected = LiberataServiceInaccessibleException.class)
    public void checkingIfExceptionThrown(){
        throwException();
    }

    @Test
    public void checkingMessageThrownByException() {
        try {
            throwException();
        } catch (LiberataServiceInaccessibleException e) {
            assertEquals("Liberata service inaccessible", e.getMessage());
        }

    }
}
