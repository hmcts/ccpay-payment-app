package uk.gov.hmcts.payment.api.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.persistence.Tuple;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Payment2RepositoryTest {

    @Mock
    private Payment2Repository payment2Repository;

    private Date fromDate;
    private Date toDate;
    private PaymentChannel paymentChannel;

    @BeforeEach
    public void setUp() {
        fromDate = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        toDate = new Date();
        paymentChannel = PaymentChannel.TELEPHONY;
    }

    @Test
    public void testFindAllByDateCreatedBetweenAndPaymentChannel_Success() {
        Tuple mockTuple1 = new CustomTuple("Service1", "CCD123", "REF123", "FEE001", new Date(), new BigDecimal("100.00"), "Success");
        Tuple mockTuple2 = new CustomTuple("Service2", "CCD456", "REF456", "FEE002", new Date(), new BigDecimal("200.00"), "Failed");

        when(payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel))
            .thenReturn(Arrays.asList(mockTuple1, mockTuple2));

        List<Tuple> results = payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel);

        assertEquals(2, results.size());
        assertEquals("Service1", results.get(0).get("service_type", String.class));
        assertEquals("CCD123", results.get(0).get("ccd_case_number", String.class));
        assertEquals("REF123", results.get(0).get("reference", String.class));
        assertEquals("Service2", results.get(1).get("service_type", String.class));
        assertEquals("CCD456", results.get(1).get("ccd_case_number", String.class));
        assertEquals("REF456", results.get(1).get("reference", String.class));
    }

    @Test
    public void testFindAllByDateCreatedBetweenAndPaymentChannel_EmptyResult() {
        when(payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel))
            .thenReturn(Arrays.asList());

        List<Tuple> results = payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel);

        assertEquals(0, results.size());
    }

    @Test
    public void testFindAllByDateCreatedBetweenAndPaymentChannel_InvalidDateRange() {
        Date invalidFromDate = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24);

        when(payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(invalidFromDate, toDate, paymentChannel))
            .thenReturn(Arrays.asList());

        List<Tuple> results = payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(invalidFromDate, toDate, paymentChannel);

        assertEquals(0, results.size());
    }

    @Test
    public void testFindAllByDateCreatedBetweenAndPaymentChannel_SpecificPaymentChannel() {
        Tuple mockTuple = new CustomTuple("Service1", "CCD123", "REF123", "FEE001", new Date(), new BigDecimal("100.00"), "Success");

        when(payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel))
            .thenReturn(Arrays.asList(mockTuple));

        List<Tuple> results = payment2Repository.findAllByDateCreatedBetweenAndPaymentChannel(fromDate, toDate, paymentChannel);

        assertEquals(1, results.size());
        assertEquals("Service1", results.get(0).get("service_type", String.class));
        assertEquals("CCD123", results.get(0).get("ccd_case_number", String.class));
        assertEquals("REF123", results.get(0).get("reference", String.class));
    }
}
