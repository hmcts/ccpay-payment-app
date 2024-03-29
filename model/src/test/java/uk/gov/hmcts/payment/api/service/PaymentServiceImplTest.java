package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.util.*;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaymentServiceImplTest {

    @InjectMocks
    @Spy
    private PaymentServiceImpl paymentService;

    @Mock
    private Payment2Repository paymentRepository;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void findAllByDateCreatedBetweenPayments() {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .build();
        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        when(paymentRepository.findAllByDateCreatedBetween(any(Date.class),any(Date.class))).thenReturn(Optional.of(paymentList));
        List<Payment> paymentList1= paymentService.getPayments(any(Date.class),any(Date.class));
        assertThat(paymentList1).isNotNull();
    }

    @Test
    public void returnEmptyListForfindAllByDateCreatedBetween() {
        when(paymentRepository.findAllByDateCreatedBetween(any(Date.class),any(Date.class))).thenReturn(Optional.of(Collections.EMPTY_LIST));
        List<Payment> paymentList1= paymentService.getPayments(any(Date.class),any(Date.class));
        assertThat(paymentList1).isEmpty();
    }

    @Test
    public void test_payment_found_and_dates_rolled_back() {

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .dateCreated(new Date())
            .dateUpdated(new Date())
            .build();
        List<Payment> paymentList = List.of(payment);
        when(paymentRepository.findByCcdCaseNumber(any())).thenReturn(Optional.of(paymentList));
        paymentService.updatePaymentsForCCDCaseNumberByCertainDays("ccdCaseNumber","5");
        verify(paymentService).updatePaymentsForCCDCaseNumberByCertainDays(anyString(), anyString());
    }

    @Test
    public void givenNoData_whenRetrievePayment_thenPaymentNotFoundExceptionReceived() {
        List<Payment> paymentList = new ArrayList<>();
        when(paymentRepository.findByReferenceIn(anyList())).thenReturn(paymentList);

        Exception exception = assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.retrievePayment(Arrays.asList("RC-1519-9028-2432-000"))
        );

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("No Payments found"));
    }

    @Test
    public void givenValidReference_whenRetrievePayment_thenPaymentsReceived() {
        List<Payment> paymentList = List.of(getPayment());
        when(paymentRepository.findByReferenceIn(anyList())).thenReturn(paymentList);

        List<Payment> payments = paymentService.retrievePayment(Arrays.asList("RC-1519-9028-2432-000"));
        assertNotNull(payments);
        assertEquals(1, payments.size());
        assertEquals("RC-1519-9028-2432-000", payments.get(0).getReference());
    }

    @Test
    public void testGetPaymentByIdSuccess() {

        when(paymentRepository.findById(anyInt())).thenReturn(Optional.of(getPayment()));
        assertNotNull(paymentService.getPaymentById(1));
    }

    @Test
    public void testGetPaymentByIdThrowError() {
        when(paymentRepository.findById(anyInt())).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(1));
    }

    @Test
    public void testFindSavedPaymentSuccess() {

        when(paymentRepository.findByReference(anyString())).thenReturn(Optional.of(getPayment()));
        assertNotNull(paymentService.findSavedPayment("Ref-1"));
    }

    @Test
    public void testFindSavedPaymentThrowError() {
        when(paymentRepository.findByReference(anyString())).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentService.findSavedPayment("Ref-1"));
    }

    @Test
    public void testFindPaymentSuccess() {

        when(paymentRepository.findByInternalReference(anyString())).thenReturn(Optional.of(getPayment()));
        assertNotNull(paymentService.findPayment("Ref-1"));
    }

    @Test
    public void testFindPaymentThrowError() {
        when(paymentRepository.findByInternalReference(anyString())).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentService.findPayment("Ref-1"));
    }

    private Payment getPayment() {
        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        PaymentAllocation paymentAllocation = PaymentAllocation.paymentAllocationWith().paymentGroupReference("2018-0000000000")
            .paymentReference("RC-1519-9028-2432-000")
            .paymentAllocationStatus(PaymentAllocationStatus.paymentAllocationStatusWith().name("Transferred").build())
            .receivingOffice("Home office")
            .reason("receiver@receiver.com")
            .explanation("sender@sender.com")
            .userId("userId")
            .build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses for ")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA0")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v")
            .reference("RC-1519-9028-2432-000")
            .statusHistories(Arrays.asList(statusHistory))
            .paymentAllocation(Arrays.asList(paymentAllocation))
            .dateCreated(new Date())
            .dateUpdated(new Date())
            .paymentLink(PaymentFeeLink.paymentFeeLinkWith()
                .paymentReference("2021-1633617464470")
                .payments(List.of(Payment.paymentWith().reference("RC-1519-9028-2432-000").build()))
                .build())
            .build();
        return payment;
    }
}
