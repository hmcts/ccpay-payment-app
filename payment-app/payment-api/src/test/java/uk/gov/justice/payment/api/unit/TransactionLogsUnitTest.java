package uk.gov.justice.payment.api.unit;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.TransactionRecord;
import uk.gov.justice.payment.api.services.PaymentService;
import uk.gov.justice.payment.api.services.SearchCriteria;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by zeeshan on 18/10/2016.
 */
@RunWith(SpringRunner.class)
@Configuration
public class TransactionLogsUnitTest extends AbstractPaymentTest {

    @Mock
    PaymentService paymentService;
    @Mock
    SearchCriteria searchCriteria;
    @Mock
    List<TransactionRecord> txList;
    @Test
    public void searchTransactionsNotFound() throws FileNotFoundException {

        MockitoAnnotations.initMocks(this);
        List<TransactionRecord> txList = new ArrayList<TransactionRecord>();
        when(paymentService.searchPayment(searchCriteria)).thenReturn(txList);
        ReflectionTestUtils.setField(paymentController,"paymentService",paymentService);
        ReflectionTestUtils.setField(paymentController,"restTemplate",restTemplate);
        ReflectionTestUtils.setField(paymentController,"mapper",mapper);
        assertEquals(paymentController.searchPayment(null,null,null,null,null,null,null).getStatusCode().value(),404);


    }
    @Test
    public void searchTransactions() throws FileNotFoundException {

        MockitoAnnotations.initMocks(this);
        when(paymentService.searchPayment(any())).thenReturn(txList);
        when(txList.size()).thenReturn(2);
        ReflectionTestUtils.setField(paymentController,"paymentService",paymentService);
        ReflectionTestUtils.setField(paymentController,"restTemplate",restTemplate);
        ReflectionTestUtils.setField(paymentController,"mapper",mapper);
        assertEquals(paymentController.searchPayment(null,null,null,null,null,null,null).getStatusCode().value(),200);


    }
}
