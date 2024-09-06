package uk.gov.hmcts.payment.api.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class Payment2RepositoryTest {

    @Mock
    private Payment2Repository payment2Repository;

    private Payment payment1;
    private Payment payment2;

    @BeforeEach
    public void setUp() {
        // Set up test data
        payment1 = new Payment();
        payment1.setServiceType("Service1");
        payment1.setCcdCaseNumber("CCD123");
        payment1.setReference("REF123");
        payment1.setDateCreated(new Date());
        payment1.setAmount(new BigDecimal("100.00"));
        payment1.setPaymentStatus(PaymentStatus.SUCCESS);
        payment1.setPaymentChannel(PaymentChannel.TELEPHONY);

        payment2 = new Payment();
        payment2.setServiceType("Service2");
        payment2.setCcdCaseNumber("CCD456");
        payment2.setReference("REF456");
        payment2.setDateCreated(new Date());
        payment2.setAmount(new BigDecimal("200.00"));
        payment2.setPaymentStatus(PaymentStatus.FAILED);
        payment2.setPaymentChannel(PaymentChannel.TELEPHONY);
    }

    @Test
    public void testFindAllByDateCreatedBetweenAndPaymentChannel() {
        Date fromDate = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        Date toDate = new Date();
        String paymentChannel = "Telephony";

        when(payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel))
            .thenReturn(List.of(
                new Object[]{"Service1", "CCD123", "REF123"},
                new Object[]{"Service2", "CCD456", "REF456"}
            ));

        List<Object[]> results = payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel);

        assertEquals(2, results.size());
        assertEquals("Service1", results.get(0)[0]);
        assertEquals("CCD123", results.get(0)[1]);
        assertEquals("REF123", results.get(0)[2]);
        assertEquals("Service2", results.get(1)[0]);
        assertEquals("CCD456", results.get(1)[1]);
        assertEquals("REF456", results.get(1)[2]);

        verify(payment2Repository, times(1)).findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel);
    }
}
