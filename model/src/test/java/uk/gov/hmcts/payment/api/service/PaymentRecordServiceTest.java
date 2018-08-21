package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PaymentRecordServiceTest {

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private PaymentStatusRepository paymentStatusRepository;

    @Mock
    private PaymentChannelRepository paymentChannelRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Spy
    private PaymentReferenceUtil paymentReferenceUtil;

    @Spy
    private PaymentMethod paymentMethod;

    @InjectMocks
    private PaymentRecordServiceImpl paymentRecordService;

    @Captor
    private ArgumentCaptor<PaymentFeeLink> argumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPopulatePaymentDetails() throws Exception {
        Payment payment = getPayment();
        List<PaymentFee> fees = Arrays.asList(getFee());

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2018-10000001")
            .payments(Arrays.asList(payment))
            .fees(fees)
            .build();

        when(paymentFeeLinkRepository.save(any(PaymentFeeLink.class))).thenReturn(paymentFeeLink);

        PaymentFeeLink savedPayment = paymentRecordService.recordPayment(payment, fees, "2018-10000001");

        verify(paymentFeeLinkRepository).save(argumentCaptor.capture());
        assertEquals(savedPayment.getPaymentReference(), paymentFeeLink.getPaymentReference());
        savedPayment.getPayments().forEach(p -> {
            assertEquals(p.getAmount(), new BigDecimal("100.11"));
            assertEquals(p.getPaymentStatus().getName(), "created");
            assertEquals(p.getPaymentChannel().getName(), "digital bar");
            assertEquals(p.getPaymentMethod().getName(), "cheque");
            assertEquals(p.getCaseReference(), "caseReference");
        });

        savedPayment.getFees().forEach(f -> {
            assertEquals(f.getCode(), "FEE0001");
            assertEquals(f.getCalculatedAmount(), new BigDecimal("100.11"));
            assertEquals(f.getVolume(), new Integer(1));
            assertEquals(f.getReference(), "caseReference");

        });
    }

    private Payment getPayment() throws CheckDigitException {
        when(paymentMethodRepository.findByNameOrThrow("cheque")).thenReturn(PaymentMethod.paymentMethodWith().name("cheque").build());
        when(paymentChannelRepository.findByNameOrThrow("digital bar")).thenReturn(PaymentChannel.paymentChannelWith().name("digital bar").build());
        when(paymentStatusRepository.findByNameOrThrow("created")).thenReturn(PaymentStatus.paymentStatusWith().name("created").build());

        return Payment.paymentWith()
            .amount(new BigDecimal("100.11"))
            .reference(paymentReferenceUtil.getNext())
            .caseReference("caseReference")
            .externalReference("chequeNumber")
            .externalProvider("cheque provider")
            .giroSlipNo("giro")
            .paymentMethod(paymentMethodRepository.findByNameOrThrow("cheque"))
            .paymentChannel(paymentChannelRepository.findByNameOrThrow("digital bar"))
            .paymentStatus(paymentStatusRepository.findByNameOrThrow("created"))
            .build();
    }

    private PaymentFee getFee() throws Exception {
        return PaymentFee.feeWith()
            .reference("caseReference")
            .code("FEE0001")
            .calculatedAmount(new BigDecimal("100.11"))
            .version("1")
            .volume(1)
            .build();
    }
}
