package uk.gov.hmcts.payment.api.exceptions;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.referencedata.exception.ReferenceDataNotFoundException;

@RunWith(SpringRunner.class)
public class PciPalClientExceptionTest {

    @Test
    public void testException(){
        Throwable msg = new Throwable("Test Cause");
        Assert.assertEquals(msg,new PciPalClientException(msg).getCause());
    }
}
