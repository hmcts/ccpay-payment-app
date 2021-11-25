package uk.gov.hmcts.payment.api.service.govpay;

import org.junit.Test;
import uk.gov.hmcts.payment.api.exceptions.PaymentServiceNotFoundException;

import static org.junit.Assert.assertEquals;

public class ServiceToTokenTest {

    ServiceToTokenMap serviceToTokenMap = new ServiceToTokenMap();

    @Test( expected=PaymentServiceNotFoundException.class)
    public void getServiceKeyVaultNameShouldThrowPaymentServiceNotFoundException() {
        serviceToTokenMap.getServiceKeyVaultName("INVALID");
    }

    @Test
    public void getServiceKeyVaultNameShouldReturnKeyvaultName(){
        String actual = serviceToTokenMap.getServiceKeyVaultName("Divorce");
        assertEquals("divorce_frontend",actual);
    }

}
