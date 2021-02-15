package uk.gov.hmcts.payment.api.service;

import org.junit.Test;
import org.mockito.InjectMocks;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

public class CardDetailsServiceTest {

    Payment2Repository paymentRespository;

    @InjectMocks
    CardDetailsServiceImpl cardDetailsService;

    @Test
    public void testRetrieve(){

    }
}
